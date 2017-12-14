package com.yanghui.elephant.remoting.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.internal.StringUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.log4j.Log4j2;

import com.yanghui.elephant.common.utils.Pair;
import com.yanghui.elephant.remoting.RemotingClient;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.common.RemotingHelper;
import com.yanghui.elephant.remoting.common.RemotingUtil;
import com.yanghui.elephant.remoting.exception.RemotingConnectException;
import com.yanghui.elephant.remoting.exception.RemotingSendRequestException;
import com.yanghui.elephant.remoting.exception.RemotingTimeoutException;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import com.yanghui.elephant.remoting.procotol.SerializeType;

@Log4j2
public class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient {
	
	private static final long LOCK_TIMEOUT_MILLIS = 3000;

	private final NettyClientConfig nettyClientConfig;
	private final Bootstrap bootstrap = new Bootstrap();
	private final EventLoopGroup eventLoopGroupWorker;
	private final Lock lockChannelTables = new ReentrantLock();
	private final ConcurrentHashMap<String, ChannelWrapper> channelTables = new ConcurrentHashMap<>();

	private DefaultEventExecutorGroup defaultEventExecutorGroup;
	
	private SerializeType serializeTypeCurrentRPC = SerializeType.HESSIAN;

	public NettyRemotingClient(NettyClientConfig nettyClientConfig) {
		this.nettyClientConfig = nettyClientConfig;
		this.eventLoopGroupWorker = new NioEventLoopGroup(1,new ThreadFactory() {
			private AtomicInteger threadIndex = new AtomicInteger(0);
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, String.format("NettyClientSelector_%d",this.threadIndex.incrementAndGet()));
			}
		});
	}

	@Override
	public void start() {
		this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(),new ThreadFactory() {
			private AtomicInteger threadIndex = new AtomicInteger(0);
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "NettyClientWorkerThread_"	+ this.threadIndex.incrementAndGet());
			}
		});
		this.bootstrap
				.group(this.eventLoopGroupWorker)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.SO_KEEPALIVE, false)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,nettyClientConfig.getConnectTimeoutMillis())
				.option(ChannelOption.SO_SNDBUF,nettyClientConfig.getClientSocketSndBufSize())
				.option(ChannelOption.SO_RCVBUF,nettyClientConfig.getClientSocketRcvBufSize())
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						// 编码
						ch.pipeline().addLast(new NettyEncoder(serializeTypeCurrentRPC));
						// 解码
						ch.pipeline().addLast(new NettyDecoder(RemotingCommand.class,serializeTypeCurrentRPC));
						// 心跳
						ch.pipeline().addLast(new IdleStateHandler(0, 0, nettyClientConfig.getClientChannelMaxIdleTimeSeconds()));
						// 业务处理
						ch.pipeline().addLast(defaultEventExecutorGroup,new NettyConnectManageHandler(), new NettyClientHandler());
					}
				});
	}
	
	
	class NettyConnectManageHandler extends ChannelDuplexHandler{
		@Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY CLIENT PIPELINE: DISCONNECT {}", remoteAddress);
            closeChannel(ctx.channel());
            super.disconnect(ctx, promise);
        }
		@Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY CLIENT PIPELINE: CLOSE {}", remoteAddress);
            closeChannel(ctx.channel());
            super.close(ctx, promise);
        }
		@Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.warn("NETTY CLIENT PIPELINE: exceptionCaught {}", remoteAddress);
            log.warn("NETTY CLIENT PIPELINE: exceptionCaught exception.", cause);
            closeChannel(ctx.channel());
        }
		@Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                    log.warn("NETTY CLIENT PIPELINE: IDLE exception [{}]", remoteAddress);
                    closeChannel(ctx.channel());
                }
            }
            ctx.fireUserEventTriggered(evt);
        }
	}

	class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {
		@Override
		public void channelReadComplete(ChannelHandlerContext ctx)throws Exception {
			ctx.flush();
		}
		@Override
		protected void channelRead0(ChannelHandlerContext ctx,RemotingCommand msg) throws Exception {
			processMessageReceived(ctx,msg);
		}
	}
	
	@Override
	public void shutdown() {
		try {
			for (ChannelWrapper cw : this.channelTables.values()) {
				this.closeChannel(cw.getChannel());
			}
			this.channelTables.clear();
			this.eventLoopGroupWorker.shutdownGracefully();
			if (this.defaultEventExecutorGroup != null) {
				this.defaultEventExecutorGroup.shutdownGracefully();
			}
		} catch (Exception e) {
			log.error("NettyRemotingClient shutdown exception, ", e);
		}
	}
	
	public void closeChannel(final String addr, final Channel channel) {
        if (null == channel){
        	return;
        }
        final String addrRemote = null == addr ? RemotingHelper.parseChannelRemoteAddr(channel) : addr;
        try {
            if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    boolean removeItemFromTable = true;
                    final ChannelWrapper prevCW = this.channelTables.get(addrRemote);

                    log.info("closeChannel: begin close the channel[{}] Found: {}", addrRemote, prevCW != null);

                    if (null == prevCW) {
                        log.info("closeChannel: the channel[{}] has been removed from the channel table before", addrRemote);
                        removeItemFromTable = false;
                    } else if (prevCW.getChannel() != channel) {
                        log.info("closeChannel: the channel[{}] has been closed before, and has been created again, nothing to do.",
                            addrRemote);
                        removeItemFromTable = false;
                    }
                    if (removeItemFromTable) {
                        this.channelTables.remove(addrRemote);
                        log.info("closeChannel: the channel[{}] was removed from channel table", addrRemote);
                    }
                    RemotingUtil.closeChannel(channel);
                } catch (Exception e) {
                    log.error("closeChannel: close the channel exception", e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            } else {
                log.warn("closeChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            log.error("closeChannel exception", e);
        }
    }

	private void closeChannel(Channel channel) {
		if (null == channel) {
			return;
		}
		try {
			if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS,TimeUnit.MILLISECONDS)) {
				try {
					boolean removeItemFromTable = true;
					ChannelWrapper prevCW = null;
					String addrRemote = null;
					for (Map.Entry<String, ChannelWrapper> entry : channelTables.entrySet()) {
						String key = entry.getKey();
						ChannelWrapper prev = entry.getValue();
						if (prev.getChannel() != null) {
							if (prev.getChannel() == channel) {
								prevCW = prev;
								addrRemote = key;
								break;
							}
						}
					}
					if (null == prevCW) {
						log.info("eventCloseChannel: the channel[{}] has been removed from the channel table before",addrRemote);
						removeItemFromTable = false;
					}
					if (removeItemFromTable) {
						this.channelTables.remove(addrRemote);
						log.info("closeChannel: the channel[{}] was removed from channel table",addrRemote);
						RemotingUtil.closeChannel(channel);
					}
				} catch (Exception e) {
					log.error("closeChannel: close the channel exception", e);
				} finally {
					this.lockChannelTables.unlock();
				}
			} else {
				log.warn("closeChannel: try to lock channel table, but timeout, {}ms",LOCK_TIMEOUT_MILLIS);
			}
		} catch (InterruptedException e) {
			log.error("closeChannel exception", e);
		}
	}

	@Override
	public void updateRegisterCenterAddressList(List<String> addrs) {
	}

	@Override
	public List<String> getRegisterCenterAddressList() {
		return null;
	}

	@Override
	public RemotingCommand invokeSync(String addr, RemotingCommand request,long timeoutMillis) 
		throws InterruptedException,RemotingSendRequestException,RemotingTimeoutException,RemotingConnectException{
		Channel channel = getAndCreateChannel(addr);
		if(channel != null && channel.isActive()){
			try{
				RemotingCommand response = this.invokeSyncImpl(channel, request, timeoutMillis);
				return response;
			}catch(RemotingSendRequestException e){
				log.warn("invokeSync: send request exception, so close the channel[{}]", addr);
                this.closeChannel(addr, channel);
                throw e;
			}catch (RemotingTimeoutException e) {
                if (nettyClientConfig.isClientCloseSocketIfTimeout()) {
                    this.closeChannel(addr, channel);
                    log.warn("invokeSync: close socket because of timeout, {}ms, {}", timeoutMillis, addr);
                }
                log.warn("invokeSync: wait response timeout exception, the channel[{}]", addr);
                throw e;
            }
		}else {
			this.closeChannel(addr, channel);
            throw new RemotingConnectException(addr);
		}
	}

	private Channel getAndCreateChannel(String addr) throws InterruptedException {
		if (StringUtil.isNullOrEmpty(addr)){
			return getAndCreateRegisterCenterChannel();
		}
		ChannelWrapper cw = this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.getChannel();
        }
        return this.createChannel(addr);
	}

	private Channel createChannel(String addr) throws InterruptedException {
		ChannelWrapper cw = this.channelTables.get(addr);
		if(cw != null && cw.isOK()){
			return cw.getChannel();
		}
		if(this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
			try {
				boolean creatNewChannel = false;
				cw = this.channelTables.get(addr);
				if(cw != null){
					if(cw.isOK()){
						return cw.getChannel();
					}else if(!cw.getChannelFuture().isDone()){
						creatNewChannel = false;
					}else {
						this.channelTables.remove(addr);
						creatNewChannel = true;
					}
				}else {
					creatNewChannel = true;
				}
				if(creatNewChannel){
					ChannelFuture channelFuture = this.bootstrap.connect(RemotingHelper.string2SocketAddress(addr));
					log.info("createChannel: begin to connect remote host[{}] asynchronously", addr);
                    cw = new ChannelWrapper(channelFuture);
                    this.channelTables.put(addr, cw);
				}
			} catch (Exception e) {
				log.error("createChannel: create channel exception", e);
			}finally{
				this.lockChannelTables.unlock();
			}
		}else{
			log.warn("createChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
		}
		if(cw != null){
			ChannelFuture channelFuture = cw.getChannelFuture();
            if (channelFuture.awaitUninterruptibly(this.nettyClientConfig.getConnectTimeoutMillis())) {
                if (cw.isOK()) {
                    log.info("createChannel: connect remote host[{}] success, {}", addr, channelFuture.toString());
                    return cw.getChannel();
                } else {
                    log.warn("createChannel: connect remote host[" + addr + "] failed, " + channelFuture.toString(), channelFuture.cause());
                }
            } else {
                log.warn("createChannel: connect remote host[{}] timeout {}ms, {}", addr, this.nettyClientConfig.getConnectTimeoutMillis(),
                    channelFuture.toString());
            }
		}
		return null;
	}

	private Channel getAndCreateRegisterCenterChannel() {
		return null;
	}

	static class ChannelWrapper {
		private final ChannelFuture channelFuture;

		public ChannelWrapper(ChannelFuture channelFuture) {
			this.channelFuture = channelFuture;
		}

		public boolean isOK() {
			return this.channelFuture.channel() != null
					&& this.channelFuture.channel().isActive();
		}

		public boolean isWriteable() {
			return this.channelFuture.channel().isWritable();
		}

		public Channel getChannel() {
			return this.channelFuture.channel();
		}

		public ChannelFuture getChannelFuture() {
			return channelFuture;
		}
	}

	@Override
	public void registerDefaultProcessor(RequestProcessor processor,ExecutorService executor) {
		this.defaultRequestProcessor = new Pair<RequestProcessor, ExecutorService>(processor, executor);
	}

	@Override
	public void invokeOneway(String addr, RemotingCommand request,long timeoutMillis)throws InterruptedException,RemotingSendRequestException,RemotingTimeoutException,RemotingConnectException {
		final Channel channel = this.getAndCreateChannel(addr);
        if (channel != null && channel.isActive()) {
            try {
                this.invokeOnewayImpl(channel, request, timeoutMillis);
            } catch (RemotingSendRequestException e) {
                log.warn("invokeOneway: send request exception, so close the channel[{}]", addr);
                this.closeChannel(addr, channel);
                throw e;
            }
        } else {
            this.closeChannel(addr, channel);
            throw new RemotingConnectException(addr);
        }
	}
}
