package com.yanghui.elephant.store.manager;

import com.yanghui.elephant.store.entity.MessageEntity;

public interface MessageEntityManager {
	
	MessageEntity findByMessageId(String messageId);

	void updateSendStatusByMessageId(int sendStatus, String messageId);

	void updateStatusByMessageId(int status, String messageId);

}
