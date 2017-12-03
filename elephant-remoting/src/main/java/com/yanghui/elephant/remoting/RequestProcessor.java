package com.yanghui.elephant.remoting;

import com.yanghui.elephant.remoting.procotol.RemotingCommand;

import io.netty.channel.ChannelHandlerContext;

public interface RequestProcessor {
	
	RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request);

}
