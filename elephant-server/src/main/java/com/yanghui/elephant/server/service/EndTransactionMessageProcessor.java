package com.yanghui.elephant.server.service;


import org.springframework.stereotype.Service;

import com.yanghui.elephant.common.protocol.header.EndTransactionRequestHeader;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class EndTransactionMessageProcessor extends AbstractRequestProcessor implements RequestProcessor {
	
	

	@Override
	public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) {
		log.info("处理结束事务消息：{}",request);
		try {
			EndTransactionRequestHeader requestHeader = (EndTransactionRequestHeader)request.decodeCommandCustomHeader(EndTransactionRequestHeader.class); 
			super.handleTransactionState(requestHeader.getMsgId(), requestHeader.getCommitOrRollback());
		} catch (Exception e) {
			log.error("EndTransaction message processor exception：{}",e);
		}
		return null;
	}
}
