package com.yanghui.elephant.server.service;

import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;

import io.netty.channel.ChannelHandlerContext;

import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;

@Service
@Log4j2
public class NettyRequestProcessor implements RequestProcessor {
	
	private 

	@Override
	public RemotingCommand processRequest(ChannelHandlerContext ctx,RemotingCommand request) {
		log.info("处理消息：{}",request);
		return null;
	}
}
