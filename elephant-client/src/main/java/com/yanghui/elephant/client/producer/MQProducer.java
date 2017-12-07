package com.yanghui.elephant.client.producer;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.common.message.Message;

public interface MQProducer {
	
	void start()throws MQClientException;
	
	void shutdown();
	
	SendResult send(Message msg) throws MQClientException;
	
	TransactionSendResult sendMessageTransaction(Message msg,LocalTransactionExecuter excuter,Object arg) throws MQClientException;

}
