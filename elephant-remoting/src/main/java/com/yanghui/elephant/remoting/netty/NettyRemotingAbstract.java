package com.yanghui.elephant.remoting.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import lombok.extern.log4j.Log4j2;

import com.yanghui.elephant.common.Pair;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.common.RemotingHelper;
import com.yanghui.elephant.remoting.exception.RemotingSendRequestException;
import com.yanghui.elephant.remoting.exception.RemotingTimeoutException;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;

@Log4j2
public abstract class NettyRemotingAbstract {
	
	protected Pair<RequestProcessor, ExecutorService> defaultRequestProcessor;
	
	protected final ConcurrentHashMap<Integer, ResponseFuture> responseTable =
	        new ConcurrentHashMap<Integer, ResponseFuture>(256);
	
	
	protected void processMessageReceived(ChannelHandlerContext ctx,RemotingCommand msg) {
		switch (msg.getType()) {
		case REQUEST_COMMAND:
			processRequestCommand(ctx, msg);
			break;
		case RESPONSE_COMMAND:
			processResponseCommand(ctx,msg);
			break;
		default:
			break;
		}
	}
	
	protected void processRequestCommand(final ChannelHandlerContext ctx,final RemotingCommand msg) {
		if(this.defaultRequestProcessor == null){
			log.warn("没有请求处理器，数据将被丢弃：{}",msg);
			return;
		}
		final RequestProcessor requestProcessor = this.defaultRequestProcessor.getObject1();
		ExecutorService executorService = this.defaultRequestProcessor.getObject2();
		if(executorService == null){
			RemotingCommand respose = requestProcessor.processRequest(ctx, msg);
			ctx.writeAndFlush(respose);
			return;
		}
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				RemotingCommand respose = requestProcessor.processRequest(ctx, msg);
				ctx.writeAndFlush(respose);
			}
		});
	}

	protected void processResponseCommand(ChannelHandlerContext ctx,RemotingCommand msg) {
		int unique = msg.getUnique();
		ResponseFuture rf = this.responseTable.get(unique);
		if(rf != null){
			rf.putResponse(msg);
		}else {
			log.warn("receive response, but not matched any request, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
            log.warn(msg);
		}
	}
	
	
	public RemotingCommand invokeSyncImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis)
			throws InterruptedException,RemotingTimeoutException,RemotingSendRequestException{
		final int unique = request.getUnique();
		try{
			final SocketAddress addr = channel.remoteAddress();
			final ResponseFuture responseFuture = new ResponseFuture(unique, timeoutMillis);
			this.responseTable.put(unique, responseFuture);
			channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if(future.isSuccess()){
						responseFuture.setSendRequestOK(true);
                        return;
					}
					responseFuture.setSendRequestOK(false);
					responseTable.remove(unique);
                    responseFuture.setCause(future.cause());
                    responseFuture.putResponse(null);
                    log.warn("send a request command to channel <" + addr + "> failed.");
				}
			});
			RemotingCommand respose = responseFuture.waitRespose(timeoutMillis);
			if(null == respose){
				if(responseFuture.isSendRequestOK()){
					throw new RemotingTimeoutException(RemotingHelper.parseSocketAddressAddr(addr), timeoutMillis, responseFuture.getCause());
				}else {
					throw new RemotingSendRequestException(RemotingHelper.parseSocketAddressAddr(addr), responseFuture.getCause());
				}
			}
			return respose;
		}finally{
			responseTable.remove(unique);
		}
	}
}
