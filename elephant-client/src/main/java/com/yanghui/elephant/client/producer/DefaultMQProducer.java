package com.yanghui.elephant.client.producer;

import lombok.Data;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.impl.DefaultMQProducerImpl;
import com.yanghui.elephant.common.message.Message;
/**
 * 普通消息生产者
 * @author --小灰灰--
 * 
 */
@Data
public class DefaultMQProducer implements MQProducer {
	
	/**
	 * 消息的最大长度（4M）
	 */
	protected int maxMessageSize = 1024 * 1024 * 4;
	/**
	 * 生产者分组
	 */
	protected String producerGroup;
	/**
	 * 发送消息的超时时间
	 */
	protected long sendMsgTimeout = 3000;
	/**
	 * 发送失败的超时次数
	 */
	protected int retryTimesWhenSendFailed = 2;
	/**
	 * 注册中心地址
	 */
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
		return this.defaultMQProducerImpl.send(msg);
	}

	@Override
	public TransactionSendResult sendMessageTransaction(Message msg,
			LocalTransactionExecuter excuter, Object arg) throws MQClientException{
		throw new RuntimeException("sendMessageInTransaction not implement, please use TransactionMQProducer class");
	}
}
