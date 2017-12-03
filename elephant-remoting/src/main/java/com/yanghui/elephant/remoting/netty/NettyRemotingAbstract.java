package com.yanghui.elephant.remoting.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.log4j.Log4j2;

import com.yanghui.elephant.remoting.common.RemotingHelper;
import com.yanghui.elephant.remoting.exception.RemotingSendRequestException;
import com.yanghui.elephant.remoting.exception.RemotingTimeoutException;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;

@Log4j2
public abstract class NettyRemotingAbstract {
	
	protected final ConcurrentHashMap<Integer, ResponseFuture> responseTable =
	        new ConcurrentHashMap<Integer, ResponseFuture>(256);
	
	
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
