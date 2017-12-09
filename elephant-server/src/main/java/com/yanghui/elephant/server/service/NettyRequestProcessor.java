package com.yanghui.elephant.server.service;

import java.util.Date;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import io.netty.channel.ChannelHandlerContext;

import com.alibaba.fastjson.JSON;
import com.yanghui.elephant.common.constant.Constant;
import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.constant.MessageStatus;
import com.yanghui.elephant.common.constant.SendStatus;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import com.yanghui.elephant.remoting.procotol.header.MessageRequestHeader;
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
		switch (request.getRemotingCommandCode()) {
		case NORMAL_MESSAGE:
			RemotingCommand response = handleMessage(request,false);
			if(response.getCode() == Constant.SUCCESS){
				/**
				 * 异步发送mq
				 */
			}
			return response;
		case TRANSACTION_PRE_MESSAGE:
			return handleMessage(request,true);
		case TRANSACTION_END_MESSAGE:
			handleTransactionEndAndCheckMessage(request);
			break;
		case TRANSACTION_CHECK_MESSAGE:
			handleTransactionEndAndCheckMessage(request);
			break;
		default:
			break;
		}
		return null;
	}
	
	private RemotingCommand handleMessage(RemotingCommand request,boolean isTransation){
		RemotingCommand response = RemotingCommand.buildResposeCmd(Constant.SERVER_FAIL, request.getUnique());
		MessageEntity entity = this.bulidMessageEntity(request,isTransation);
		int code = saveOrUpdateMassageEntity(entity,true);
		response.setCode(code);
		return response;
	}
	
	private void handleTransactionEndAndCheckMessage(RemotingCommand request){
		try {
			String messageId = request.getBody().toString();
			MessageRequestHeader header = (MessageRequestHeader)request.getCustomHeader();
			MessageEntity entity = new MessageEntity();
			entity.setMessageId(messageId);
			entity.setStatus(buildStatus(header.getLocalTransactionState()));
			entity.setRemark(request.getRemark());
			saveOrUpdateMassageEntity(entity, false);
			if(entity.getStatus() == MessageStatus.CONFIRMED.getStatus()){
				/**
				 * 异步发送mq
				 */
			}
		} catch (Exception e) {
			log.error("handleTransactionEndAndCheckMessage 发生异常：{}",e);
		}
	}
	
	private int saveOrUpdateMassageEntity(MessageEntity entity,boolean isInsert){
		try {
			MessageEntity select = new MessageEntity();
			select.setMessageId(entity.getMessageId());
			MessageEntity find = this.messageEntityMapper.selectOne(select);
			if(find == null){
				if(isInsert)this.messageEntityMapper.insert(entity);
			}else {
				MessageEntity update = new MessageEntity();
				update.setId(find.getId());
				update.setStatus(entity.getStatus());
				update.setRemark(entity.getRemark());
				update.setUpdateTime(new Date());
				this.messageEntityMapper.updateById(update);
			}
			return Constant.SUCCESS;
		} catch (Exception e) {
			log.error("保存数据库失败：{}",e);
			return Constant.FUSH_DB_FAIL;
		}
	}
	
	private MessageEntity bulidMessageEntity(RemotingCommand request,boolean isTransaction){
		Message message = (Message)request.getBody();
		MessageRequestHeader header = (MessageRequestHeader)request.getCustomHeader();
		MessageEntity entity = new MessageEntity();
		entity.setBody(message.getBody());
		entity.setCreateTime(new Date());
		entity.setDestination(message.getDestination());
		entity.setGroup(header.getGroup());
		entity.setMessageId(message.getMessageId());
		if(!CollectionUtils.isEmpty(message.getProperties())){
			entity.setProperties(JSON.toJSONString(message.getProperties()));
		}
		entity.setSendStatus(SendStatus.WAIT_SEND.getStatus());
		entity.setTransaction(isTransaction);
		entity.setRemark(request.getRemark());
		entity.setUpdateTime(entity.getCreateTime());
		if(!entity.getTransaction()){
			entity.setStatus(MessageStatus.CONFIRMED.getStatus());
			return entity;
		}
		
		entity.setStatus(buildStatus(header.getLocalTransactionState()));
		return entity;
	}
	
	private int buildStatus(LocalTransactionState localTransactionState){
		switch (localTransactionState) {
		case PRE_MESSAGE:
			return MessageStatus.CONFIRMING.getStatus();
		case COMMIT_MESSAGE:
			return MessageStatus.CONFIRMED.getStatus();
		case ROLLBACK_MESSAGE:
			return MessageStatus.ROLLBACK.getStatus();
		case UNKNOW:
			return MessageStatus.CONFIRMING.getStatus();
		default:
			return MessageStatus.CONFIRMING.getStatus();
		}
	}
}
