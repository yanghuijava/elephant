package com.yanghui.elephant.remoting.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.log4j.Log4j2;

import com.yanghui.elephant.common.Pair;
import com.yanghui.elephant.remoting.RemotingServer;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import com.yanghui.elephant.remoting.procotol.SerializeType;

@Log4j2
public class NettyRemotingServer implements RemotingServer {

	private final ServerBootstrap serverBootstrap;
	private final EventLoopGroup eventLoopGroupSelector;
	private final EventLoopGroup eventLoopGroupBoss;
	private final NettyServerConfig nettyServerConfig;

	private DefaultEventExecutorGroup defaultEventExecutorGroup;
	
	private int port = 0;
	
	private SerializeType serializeTypeCurrentRPC = SerializeType.HESSIAN;
	
	private Pair<RequestProcessor, ExecutorService> defaultRequestProcessor;

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
	}
	
	class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {
		
		@Override
	    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
	        ctx.flush();
	    }
		
		@Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
	        cause.printStackTrace();
	        //发生异常,关闭链路
	        ctx.close();
	    }

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg)	throws Exception {
			processMessageReceived(ctx, msg);
		}
	}
	
	private void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg){
		switch (msg.getType()) {
		case REQUEST_COMMAND:
			processRequestCommand(ctx,msg);
			break;
		case RESPONSE_COMMAND:
			
			break;
		default:
			break;
		}
	}

	private void processRequestCommand(ChannelHandlerContext ctx,RemotingCommand msg) {
		if(this.defaultRequestProcessor == null){
			log.warn("没有请求处理器，数据将被丢弃：{}",msg);
			return;
		}
		RemotingCommand respose = this.defaultRequestProcessor.getObject1().processRequest(ctx, msg);
		ctx.writeAndFlush(respose);
	}

	@Override
	public void shutdown() {
		this.eventLoopGroupBoss.shutdownGracefully();

        this.eventLoopGroupSelector.shutdownGracefully();
        
        this.defaultEventExecutorGroup.shutdownGracefully();
	}

	@Override
	public void registerDefaultProcessor(RequestProcessor processor,ExecutorService executor) {
		this.defaultRequestProcessor = new Pair<RequestProcessor, ExecutorService>(processor, executor);
	}

	@Override
	public int localListenPort() {
		return this.port;
	}
}
