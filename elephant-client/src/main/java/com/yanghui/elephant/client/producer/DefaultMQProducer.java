package com.yanghui.elephant.client.producer;

import lombok.Data;

import com.yanghui.elephant.common.message.Message;
@Data
public class DefaultMQProducer implements MQProducer {
	
	private int maxMessageSize = 1024 * 1024 * 4; // 4M
	
	private String producerGroup;
	
	private long sendMsgTimeout = 3000;
	
	private String registerCenter;

	@Override
	public void start() {

	}

	@Override
	public void shutdown() {

	}

	@Override
	public SendResult send(Message msg) {
		return null;
	}

	@Override
	public TransactionSendResult sendMessageTransaction(Message msg,
			LocalTransactionExecuter excuter, Object arg) {
		throw new RuntimeException("sendMessageInTransaction not implement, please use TransactionMQProducer class");
	}
}
