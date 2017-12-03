package com.yanghui.elephant.server.service;

import java.util.Date;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import io.netty.channel.ChannelHandlerContext;

import com.alibaba.fastjson.JSON;
import com.yanghui.elephant.common.constant.Constant;
import com.yanghui.elephant.common.constant.MessageStatus;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import com.yanghui.elephant.store.entity.MessageEntity;
import com.yanghui.elephant.store.mapper.MessageEntityMapper;

@Service
@Log4j2
public class NettyRequestProcessor implements RequestProcessor {
	
	@Autowired
	private MessageEntityMapper messageEntityMapper;
	
	@Override
	public RemotingCommand processRequest(ChannelHandlerContext ctx,RemotingCommand request) {
		log.info("处理消息：{}",request);
		RemotingCommand response = RemotingCommand.buildResposeCmd(Constant.SERVER_FAIL, request.getUnique());
		if(!(request.getBody() instanceof Message)){
			return response;
		}
		try {
			MessageEntity entity = this.bulidMessageEntity(request);
			saveOrUpdateMassageEntity(entity);
			/**
			 * 消息确认后，发送消息到mq，待实现
			 */
			if(entity.getStatus() == MessageStatus.CONFIRMED.getStatus()){
				
			}
			response.setCode(Constant.SUCCESS);
		} catch (Exception e) {
			log.error("处理消息发送异常：{}",e);
		}
		return response;
	}
	
	private void saveOrUpdateMassageEntity(MessageEntity entity){
		MessageEntity select = new MessageEntity();
		select.setMessageId(entity.getMessageId());
		MessageEntity find = this.messageEntityMapper.selectOne(select);
		if(find == null){
			this.messageEntityMapper.insert(entity);
		}else {
			MessageEntity update = new MessageEntity();
			update.setId(find.getId());
			update.setStatus(entity.getStatus());
			update.setUpdateTime(new Date());
			this.messageEntityMapper.updateById(update);
		}
	}
	
	private MessageEntity bulidMessageEntity(RemotingCommand request){
		Message message = (Message)request.getBody();
		MessageEntity entity = new MessageEntity();
		entity.setBody(message.getBody());
		entity.setCreateTime(new Date());
		entity.setDestination(message.getDestination());
		entity.setGroup(request.getGroup());
		entity.setMessageId(message.getMessageId());
		if(!CollectionUtils.isEmpty(message.getProperties())){
			entity.setProperties(JSON.toJSONString(message.getProperties()));
		}
		entity.setUpdateTime(entity.getCreateTime());
		switch (request.getLocalTransactionState()) {
		case PRE_MESSAGE:
			entity.setStatus(MessageStatus.CONFIRMING.getStatus());
			break;
		case COMMIT_MESSAGE:
			entity.setStatus(MessageStatus.CONFIRMED.getStatus());
			break;
		case ROLLBACK_MESSAGE:
			entity.setStatus(MessageStatus.ROLLBACK.getStatus());
			break;
		case UNKNOW:
			entity.setStatus(MessageStatus.CONFIRMING.getStatus());
			break;
		default:
			entity.setStatus(MessageStatus.CONFIRMING.getStatus());
			break;
		}
		return entity;
	}
}
