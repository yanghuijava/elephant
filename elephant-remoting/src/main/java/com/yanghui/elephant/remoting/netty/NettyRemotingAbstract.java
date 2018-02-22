package com.yanghui.elephant.remoting.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import lombok.extern.log4j.Log4j2;

import com.yanghui.elephant.common.utils.Pair;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.common.RemotingHelper;
import com.yanghui.elephant.remoting.exception.RemotingSendRequestException;
import com.yanghui.elephant.remoting.exception.RemotingTimeoutException;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
/**
 * 
 * @author --小灰灰--
 *
 */
@Log4j2
public abstract class NettyRemotingAbstract {
	
	protected Pair<RequestProcessor, ExecutorService> defaultRequestProcessor;
	
	protected Map<Integer, Pair<RequestProcessor, ExecutorService>> processorTable = 
				new HashMap<Integer, Pair<RequestProcessor, ExecutorService>>(64);
	
	protected final ConcurrentHashMap<Integer, ResponseFuture> responseTable =
	        new ConcurrentHashMap<Integer, ResponseFuture>(256);
	
	/**
	 * 扫描响应列表，超时的删除
	 */
	protected void scanResponseTable() {
		Iterator<Entry<Integer, ResponseFuture>> it = responseTable.entrySet().iterator();
		while(it.hasNext()) {
     	  Entry<Integer, ResponseFuture> entry = it.next();
     	  ResponseFuture rf = entry.getValue();
     	   if((rf.getBeginTimestamp() + rf.getTimeoutMillis() + 1000) <= System.currentTimeMillis()) {
     		   it.remove();
     		   log.warn("remove timeout request, {}",entry.getValue());
     	   }
        }
	}
	
	protected void processMessageReceived(ChannelHandlerContext ctx,RemotingCommand cmd) {
		switch (cmd.getRemotingCommandType()) {
		case REQUEST_COMMAND:
			processRequestCommand(ctx, cmd);
			break;
		case RESPONSE_COMMAND:
			processResponseCommand(ctx,cmd);
			break;
		default:
			break;
		}
	}
	
	private void handlerRequestProcessor(final ChannelHandlerContext ctx,final RemotingCommand request,Pair<RequestProcessor, ExecutorService> pair) {
		final RequestProcessor requestProcessor = pair.getObject1();
		ExecutorService executorService = pair.getObject2();
		if(executorService == null){
			RemotingCommand respose = requestProcessor.processRequest(ctx, request);
			if(respose != null)ctx.writeAndFlush(respose);
			return;
		}
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				RemotingCommand respose = requestProcessor.processRequest(ctx, request);
				if(respose != null)ctx.writeAndFlush(respose);
			}
		});
	}
	
	protected void processRequestCommand(final ChannelHandlerContext ctx,final RemotingCommand request) {
		if(this.processorTable.get(request.getCode()) != null){
			handlerRequestProcessor(ctx, request, this.processorTable.get(request.getCode()));
			return;
		}
		if(this.defaultRequestProcessor != null){
			handlerRequestProcessor(ctx, request, this.defaultRequestProcessor);
			return;
		}
		log.warn("没有请求处理器，数据将被丢弃：{}",request);
	}

	protected void processResponseCommand(ChannelHandlerContext ctx,RemotingCommand msg) {
		int unique = msg.getUnique();
		ResponseFuture rf = this.responseTable.get(unique);
		if(rf != null){
			rf.putResponse(msg);
			this.responseTable.remove(unique);
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
			this.responseTable.remove(unique);
		}
	}
	
	public void invokeOnewayImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis)
	        throws RemotingTimeoutException, RemotingSendRequestException {
        try {
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (!f.isSuccess()) {
                        log.warn("send a request command to channel <" + channel.remoteAddress() + "> failed.");
                    }
                }
            });
        } catch (Exception e) {
            log.warn("write send a request command to channel <" + channel.remoteAddress() + "> failed.");
            throw new RemotingSendRequestException(RemotingHelper.parseChannelRemoteAddr(channel), e);
        }
    }
}
