package com.yanghui.elephant.remoting.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.log4j.Log4j2;

import com.yanghui.elephant.common.utils.Pair;
import com.yanghui.elephant.remoting.RemotingServer;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.common.RemotingUtil;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import com.yanghui.elephant.remoting.procotol.SerializeType;
import com.yanghui.elephant.remoting.procotol.header.MessageRequestHeader;

@Log4j2
public class NettyRemotingServer extends NettyRemotingAbstract implements RemotingServer {

	private static final long LOCK_TIMEOUT_MILLIS = 1000;
	private final Lock lockChannelTables = new ReentrantLock();
	
	private final ServerBootstrap serverBootstrap;
	private final EventLoopGroup eventLoopGroupSelector;
	private final EventLoopGroup eventLoopGroupBoss;
	private final NettyServerConfig nettyServerConfig;

	private DefaultEventExecutorGroup defaultEventExecutorGroup;
	
	private ScheduledExecutorService removeExpireKeyExecutor;
	
	private volatile ConcurrentHashMap<String,Set<Channel>> channelMap = new ConcurrentHashMap<String, Set<Channel>>();
	
	private int port = 0;
	
	private SerializeType serializeTypeCurrentRPC = SerializeType.HESSIAN;
	
	public NettyRemotingServer(final NettyServerConfig nettyServerConfig) {
		this.nettyServerConfig = nettyServerConfig;
		this.serverBootstrap = new ServerBootstrap();
		this.eventLoopGroupBoss = new NioEventLoopGroup(1, new ThreadFactory() {
			private AtomicInteger threadIndex = new AtomicInteger(0);
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, String.format("NettyBoss_%d",this.threadIndex.incrementAndGet()));
			}
		});
		this.eventLoopGroupSelector = new NioEventLoopGroup(
		nettyServerConfig.getServerSelectorThreads(),
		new ThreadFactory() {
			private AtomicInteger threadIndex = new AtomicInteger(0);
			private int threadTotal = nettyServerConfig.getServerSelectorThreads();
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, String.format("NettyServerEPOLLSelector_%d_%d", threadTotal,this.threadIndex.incrementAndGet()));
			}
		});
		this.removeExpireKeyExecutor = Executors.newSingleThreadScheduledExecutor();
	}

	@Override
	public void start() {
		this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyServerConfig.getServerWorkerThreads(),new ThreadFactory() {
			private AtomicInteger threadIndex = new AtomicInteger(0);
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "NettyServerCodecThread_" + this.threadIndex.incrementAndGet());
			}
		});
		ServerBootstrap childHandler = this.serverBootstrap
				.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024)
				.option(ChannelOption.SO_REUSEADDR, true)
				.option(ChannelOption.SO_KEEPALIVE, false)
				.childOption(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.SO_SNDBUF,nettyServerConfig.getServerSocketSndBufSize())
				.option(ChannelOption.SO_RCVBUF,nettyServerConfig.getServerSocketRcvBufSize())
				.localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort()))
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						//编码
						ch.pipeline().addLast(new NettyEncoder(serializeTypeCurrentRPC));
						//解码
						ch.pipeline().addLast(new NettyDecoder(RemotingCommand.class,serializeTypeCurrentRPC));
						//心跳
						ch.pipeline().addLast(new IdleStateHandler(0, 0, nettyServerConfig.getServerChannelMaxIdleTimeSeconds()));
						//业务处理
						ch.pipeline().addLast(defaultEventExecutorGroup,"nettyServerHandler",new NettyServerHandler());
					}
				});
		if (nettyServerConfig.isServerPooledByteBufAllocatorEnable()) {
            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
		try {
            ChannelFuture sync = this.serverBootstrap.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) sync.channel().localAddress();
            this.port = addr.getPort();
            log.info("netty server already started！monitor at port {}",this.port);
        } catch (InterruptedException e1) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e1);
        }
		this.removeExpireKeyExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				log.debug(channelMap);
				try {
					if(channelMap.isEmpty()){
						return;
					}
					for(Entry<String,Set<Channel>> entry : channelMap.entrySet()){
						if(entry.getValue() == null){
							continue;
						}
						Set<Channel> removeSet = new HashSet<Channel>();
						for(Channel c : entry.getValue()){
							if(c.isActive()){
								continue;
							}
							removeSet.add(c);
						}
						entry.getValue().removeAll(removeSet);
					}
				} catch (Exception e) {
					log.error("removeExpireKeyExecutor error:{}",e);
				}
			}
		}, 1000,3000,TimeUnit.MILLISECONDS);
	}
	
	class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {
		
		@Override
	    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
	        ctx.flush();
	    }
		
		@Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			log.warn("NETTY SERVER PIPELINE: exceptionCaught {}", ctx.channel().remoteAddress());
//            log.warn("NETTY SERVER PIPELINE: exceptionCaught exception.", cause);
            RemotingUtil.closeChannel(ctx.channel());
	    }
		
		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt)throws Exception {
			if (evt instanceof IdleStateEvent) {
                IdleStateEvent evnet = (IdleStateEvent) evt;
                if (evnet.state().equals(IdleState.ALL_IDLE)) {
                    log.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", ctx.channel().remoteAddress());
                    RemotingUtil.closeChannel(ctx.channel());
                }
            }
            ctx.fireUserEventTriggered(evt);
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg)	throws Exception {
			saveChannel(ctx.channel(), msg);
			processMessageReceived(ctx, msg);
		}
	}
	
	private void saveChannel(Channel channel,RemotingCommand msg){
		MessageRequestHeader header = (MessageRequestHeader)msg.getCustomHeader();
		try {
			Set<Channel> set = this.channelMap.get(header.getGroup());
			if(set != null){
				set.add(channel);
				return;
			}
			try{
				if(lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)){
					set = this.channelMap.get(header.getGroup());
					if(set == null){
						set = new HashSet<Channel>();
						channelMap.put(header.getGroup(),set);
					}
					set.add(channel);
				}
			}finally{
				lockChannelTables.unlock();
			}
		} catch (InterruptedException e) {
			log.error("server save channel error:{}",e);
		}
	}
	
	@Override
	public void shutdown() {
		this.eventLoopGroupBoss.shutdownGracefully();

        this.eventLoopGroupSelector.shutdownGracefully();
        
        this.defaultEventExecutorGroup.shutdownGracefully();
        
        this.removeExpireKeyExecutor.shutdown();
	}

	@Override
	public void registerDefaultProcessor(RequestProcessor processor,ExecutorService executor) {
		this.defaultRequestProcessor = new Pair<RequestProcessor, ExecutorService>(processor, executor);
	}

	@Override
	public int localListenPort() {
		return this.port;
	}

	@Override
	public void sendToClient(RemotingCommand request) {
		MessageRequestHeader header = (MessageRequestHeader)request.getCustomHeader();
		Set<Channel> set = this.channelMap.get(header.getGroup());
		if(set == null){
			return;
		}
		for(Channel channel : set){
			if(!channel.isActive()){
				continue;
			}
			channel.writeAndFlush(request);
			break;
		}
	}
}
