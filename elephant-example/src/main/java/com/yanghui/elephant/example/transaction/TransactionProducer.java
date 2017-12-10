package com.yanghui.elephant.example.transaction;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.producer.LocalTransactionExecuter;
import com.yanghui.elephant.client.producer.TransactionMQProducer;
import com.yanghui.elephant.client.producer.TransactionSendResult;
import com.yanghui.elephant.common.message.Message;

public class TransactionProducer {
	
	public static void main(String[] args) throws MQClientException {
		TransactionMQProducer producer = new TransactionMQProducer("transaction-test");
		producer.setRegisterCenter("172.16.21.12:2181");
		producer.start();
		producer.setTransactionCheckListener(new TransactionCheckListenerImpl());
		LocalTransactionExecuter excuter = new LocalTransactionExecuterImpl();
		for(int i=0;i<1;i++){
			Message msg = new Message("topic://VirtualTopic.Test", ("我是事务消息" + i).getBytes());
			TransactionSendResult  transactionSendResult  = producer.sendMessageTransaction(msg, excuter, null);
			System.out.println(transactionSendResult);
		}
//		producer.shutdown();
	}
}
