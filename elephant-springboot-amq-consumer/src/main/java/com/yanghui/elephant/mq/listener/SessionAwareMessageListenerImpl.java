package com.yanghui.elephant.mq.listener;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import lombok.extern.log4j.Log4j2;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.SessionAwareMessageListener;

import com.yanghui.elephant.mq.constant.MQConstant;

@Log4j2
public class SessionAwareMessageListenerImpl implements SessionAwareMessageListener<Message>{

	private static final ConcurrentHashMap<String,AtomicInteger> EXCETIONCOUNT 
						= new ConcurrentHashMap<String,AtomicInteger>();
	
	private Object bean;
	private Method method;
	private int retryTimes;
	private JmsTemplate jmsTemplate;
	private boolean isRetry;
	public SessionAwareMessageListenerImpl(Object bean, Method method,int retryTimes,boolean isRetry,JmsTemplate jmsTemplate) {
		this.bean = bean;
		this.method = method;
		this.retryTimes = retryTimes;
		this.isRetry = isRetry;
		this.jmsTemplate = jmsTemplate;
	}
	@Override
	public void onMessage(Message message, Session session) throws JMSException {
		try {
			Class<?>[] clzs = method.getParameterTypes();
			if(clzs != null && clzs.length == 1){
				Class<?> clz = clzs[0];
				if(clz.getName().equals(Message.class.getName())){
					this.method.invoke(this.bean, message);
				}
			}
			message.acknowledge();
			EXCETIONCOUNT.remove(message.getJMSMessageID());
		} catch (Throwable e) {
			log.error("消费消息发生异常：{}",e);
			if(isRetry){
				session.recover();
				return;
			}
			AtomicInteger ci = EXCETIONCOUNT.get(message.getJMSMessageID());
			if(ci == null){
				ci = new AtomicInteger();
				EXCETIONCOUNT.put(message.getJMSMessageID(), ci);
			}
			int count = ci.getAndIncrement();
			if(count >= this.retryTimes){
				try {
					Destination destination = message.getJMSDestination();
					if(destination instanceof ActiveMQQueue){
						this.jmsTemplate.send(new ActiveMQQueue(((ActiveMQQueue) destination).getQueueName() + MQConstant.RETRY_QUEUE_SUFFIX), 
								new MessageCreator() {
							@Override
							public Message createMessage(Session session) throws JMSException {
								return message;
							}
						});
					}else if(destination instanceof ActiveMQTopic){
						this.jmsTemplate.send(new ActiveMQTopic(((ActiveMQTopic) destination).getTopicName() + MQConstant.RETRY_QUEUE_SUFFIX), 
								new MessageCreator() {
							@Override
							public Message createMessage(Session session) throws JMSException {
								return message;
							}
						});
					}
					message.acknowledge();
					EXCETIONCOUNT.remove(message.getJMSMessageID());
				} catch (Throwable e2) {
					log.error("重试次数到了，发送消息到重试队列失败：{}",e2);
					ci.getAndDecrement();
					session.recover();
				}
			}else {
				session.recover();
			}
		}
	}
}
