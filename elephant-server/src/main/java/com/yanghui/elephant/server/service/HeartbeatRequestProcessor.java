package com.yanghui.elephant.server.service;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.netty.channel.ChannelHandlerContext;

import com.yanghui.elephant.common.constant.ResponseCode;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;

@Service
@Log4j2
public class HeartbeatRequestProcessor implements RequestProcessor{
	
	@Autowired
	private ProducerManager producerManager;

	@Override
	public RemotingCommand processRequest(ChannelHandlerContext ctx,RemotingCommand request) {
		log.debug("Receive the heartbeat from the client:{}，request：{}", ctx.channel().remoteAddress(),request);
		if(!StringUtils.isEmpty(request.getRemark())){
			String[] groups = request.getRemark().split(",");
			for(String group : groups){
				this.producerManager.registerProducer(group,ctx.channel());
			}
		}
		RemotingCommand response = RemotingCommand.buildResposeCmd(ResponseCode.SUCCESS, request.getUnique());
		return response;
	}
}
