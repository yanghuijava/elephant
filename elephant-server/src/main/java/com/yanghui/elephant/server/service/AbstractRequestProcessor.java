package com.yanghui.elephant.server.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;
import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.constant.MessageStatus;
import com.yanghui.elephant.common.constant.SendStatus;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.common.utils.StringUtil;
import com.yanghui.elephant.mq.producer.ProducerService;
import com.yanghui.elephant.store.entity.MessageEntity;
import com.yanghui.elephant.store.manager.MessageEntityManager;

public abstract class AbstractRequestProcessor {
	
	@Autowired
	protected MessageEntityManager messageEntityManager;
	@Autowired
	protected ProducerService producerService;
	
	@SuppressWarnings("unchecked")
	protected void handleTransactionState(String messageId,String commitOrRollback) {
		LocalTransactionState localState = LocalTransactionState.valueOfName(commitOrRollback);
		
		MessageEntity findMessageEntity = this.messageEntityManager.findByMessageId(messageId);
		
		boolean sendMq = false;
		switch (localState) {
		case COMMIT_MESSAGE:
			this.messageEntityManager.updateStatusByMessageId(MessageStatus.CONFIRMED.getStatus(),findMessageEntity.getMessageId());
			sendMq = true;
			break;
		case ROLLBACK_MESSAGE:
			this.messageEntityManager.updateStatusByMessageId(MessageStatus.ROLLBACK.getStatus(),findMessageEntity.getMessageId());
			break;
		default:
			break;
		}
		if(sendMq) {
			Message message = new Message();
			message.setBody(findMessageEntity.getBody());
			message.setDestination(findMessageEntity.getDestination());
			message.setMessageId(findMessageEntity.getMessageId());
			if(!StringUtil.isEmpty(findMessageEntity.getProperties())) {
				message.setProperties((Map<String, String>)JSON.parseObject(findMessageEntity.getProperties(),Map.class));
			}
			this.producerService.sendMessage(message);
			this.messageEntityManager.updateSendStatusByMessageId(SendStatus.ALREADY_SEND.getStatus(),
					findMessageEntity.getMessageId());
		}
	}
}
