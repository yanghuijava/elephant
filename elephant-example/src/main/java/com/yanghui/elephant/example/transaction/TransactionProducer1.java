package com.yanghui.elephant.example.transaction;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.producer.LocalTransactionExecuter;
import com.yanghui.elephant.client.producer.TransactionMQProducer;
import com.yanghui.elephant.client.producer.TransactionSendResult;
import com.yanghui.elephant.common.message.Message;

public class TransactionProducer1 {
	
	public static void main(String[] args) throws MQClientException {
		TransactionMQProducer producer1 = new TransactionMQProducer("transaction-test1");
		producer1.setRegisterCenter("172.16.21.12:2181");
		producer1.setTransactionCheckListener(new TransactionCheckListenerImpl());
		producer1.start();
		
		TransactionMQProducer producer2 = new TransactionMQProducer("transaction-test2");
		producer2.setRegisterCenter("172.16.21.12:2181");
		producer2.setTransactionCheckListener(new TransactionCheckListenerImpl2());
		producer2.start();
		
		LocalTransactionExecuter excuter = new LocalTransactionExecuterImpl();
		
		Message msg1 = new Message("topic://VirtualTopic.Test", ("我是事务消息1").getBytes());
		TransactionSendResult  transactionSendResult1  = producer1.sendMessageTransaction(msg1, excuter, null);
		System.out.println("transactionSendResult1=" + transactionSendResult1);
		
		Message msg2 = new Message("topic://VirtualTopic.Test", ("我是事务消息2").getBytes());
		TransactionSendResult  transactionSendResult2  = producer2.sendMessageTransaction(msg2, excuter, null);
		System.out.println("transactionSendResult2=" + transactionSendResult2);
	}
}
