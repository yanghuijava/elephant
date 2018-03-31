package com.yanghui.elephant.example.transaction;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.producer.LocalTransactionExecuter;
import com.yanghui.elephant.client.producer.TransactionMQProducer;
import com.yanghui.elephant.client.producer.TransactionSendResult;
import com.yanghui.elephant.common.message.Message;

public class TransactionProducer {
	
	public static void main(String[] args) throws MQClientException {
		TransactionMQProducer producer = new TransactionMQProducer("transaction-test");
		producer.setRegisterCenter("120.77.152.143:2181");
		producer.setTransactionCheckListener(new TransactionCheckListenerImpl());
		producer.start();
		LocalTransactionExecuter excuter = new LocalTransactionExecuterImpl();
		/**
		 * 目前只支持activemq：
		 * 发送queue，message的destination值加上前缀：queue://
		 * 发送topic，message的destination值加上前缀：topic://
		 */
		for(int i=0;i<1;i++){
			Message msg = new Message("queue://yanghui.queue.test1", ("我是事务消息" + i).getBytes());
			TransactionSendResult  transactionSendResult  = producer.sendMessageTransaction(msg, excuter, null);
			System.out.println(transactionSendResult);
		}
//		producer.shutdown();
	}
}
