package com.yanghui.elephant.mq.producer.impl;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.util.CollectionUtils;

import lombok.Data;

import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.common.utils.StringUtil;
import com.yanghui.elephant.mq.producer.ProducerService;

@Data
public class ActivemqProducerService implements ProducerService {
	
	private static final String QUEUE_PREFIX = "queue://";
	private static final String TOPIC_PREFIX = "topic://";
	
	private JmsTemplate jmsTemplate;

	@Override
	public void sendMessage(final Message message) {
		this.jmsTemplate.send(createDestination(message.getDestination()), new MessageCreator() {
			@Override
			public javax.jms.Message createMessage(Session session) throws JMSException {
				BytesMessage bytesMessage = session.createBytesMessage();
				bytesMessage.writeBytes(message.getBody());
				if(!CollectionUtils.isEmpty(message.getProperties())){
					if(message.getProperties().get("JMSXGroupID") != null){
						bytesMessage.setStringProperty("JMSXGroupID", message.getProperties().get("JMSXGroupID").toString());
					}
					if(message.getProperties().get("JMSXGroupSeq") != null){
						String JMSXGroupSeq = message.getProperties().get("JMSXGroupSeq").toString();
						if(StringUtil.isNumeric(JMSXGroupSeq)){
							bytesMessage.setIntProperty("JMSXGroupSeq", Integer.valueOf(JMSXGroupSeq));
						}
					}
				}
				return bytesMessage;
			}
		});
	}
	
	private Destination createDestination(String destination){
		if(destination.contains(QUEUE_PREFIX)){
			return new ActiveMQQueue(destination.replace(QUEUE_PREFIX, ""));
		}else if(destination.contains(TOPIC_PREFIX)){
			return new ActiveMQTopic(destination.replace(TOPIC_PREFIX, ""));
		}
		return new ActiveMQQueue(destination);
	}
}
