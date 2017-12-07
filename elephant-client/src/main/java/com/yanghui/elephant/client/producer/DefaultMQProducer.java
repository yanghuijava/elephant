package com.yanghui.elephant.client.producer;

import lombok.Data;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.impl.DefaultMQProducerImpl;
import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.remoting.procotol.header.MessageRequestHeader;
@Data
public class DefaultMQProducer implements MQProducer {
	
	protected int maxMessageSize = 1024 * 1024 * 4; // 4M
	
	protected String producerGroup;
	
	protected long sendMsgTimeout = 3000;
	
	protected String registerCenter;
	
	protected DefaultMQProducerImpl defaultMQProducerImpl;
	
	public DefaultMQProducer(){
		this(null);
	}
	
	public DefaultMQProducer(String producerGroup){
		this.producerGroup = producerGroup;
		this.defaultMQProducerImpl = new DefaultMQProducerImpl(this);
	}

	@Override
	public void start() throws MQClientException{
		this.defaultMQProducerImpl.start();
	}

	@Override
	public void shutdown() {
		this.defaultMQProducerImpl.shutdown();
	}

	@Override
	public SendResult send(Message msg) throws MQClientException {
		MessageRequestHeader header = new MessageRequestHeader();
		header.setGroup(this.producerGroup);
		header.setLocalTransactionState(LocalTransactionState.COMMIT_MESSAGE);
		return this.defaultMQProducerImpl.send(msg, header);
	}

	@Override
	public TransactionSendResult sendMessageTransaction(Message msg,
			LocalTransactionExecuter excuter, Object arg) throws MQClientException{
		throw new RuntimeException("sendMessageInTransaction not implement, please use TransactionMQProducer class");
	}
}
