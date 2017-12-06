package com.yanghui.elephant.client.producer;

import com.yanghui.elephant.common.message.Message;

public interface MQProducer {
	
	void start();
	
	void shutdown();
	
	SendResult send(Message msg);
	
	TransactionSendResult sendMessageTransaction(Message msg,LocalTransactionExecuter excuter,Object arg);

}
