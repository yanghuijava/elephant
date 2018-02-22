package com.yanghui.elephant.store.manager.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yanghui.elephant.store.entity.MessageEntity;
import com.yanghui.elephant.store.manager.MessageEntityManager;
import com.yanghui.elephant.store.mapper.MessageEntityMapper;

@Service
public class MessageEntityManagerImpl implements MessageEntityManager {
	
	@Autowired
	private MessageEntityMapper messageEntityMapper;

	@Override
	public MessageEntity findByMessageId(String messageId) {
		MessageEntity select = new MessageEntity();
		select.setMessageId(messageId);
		MessageEntity find = this.messageEntityMapper.selectOne(select);
		return find;
	}

	@Override
	public void updateSendStatusByMessageId(int sendStatus, String messageId) {
		MessageEntity update = new MessageEntity();
		update.setMessageId(messageId);
		update.setSendStatus(sendStatus);
		update.setUpdateTime(new Date());
		this.messageEntityMapper.updateByMessageId(update);
	}

	@Override
	public void updateStatusByMessageId(int status, String messageId) {
		MessageEntity update = new MessageEntity();
		update.setMessageId(messageId);
		update.setStatus(status);
		update.setUpdateTime(new Date());
		this.messageEntityMapper.updateByMessageId(update);
	}
}
