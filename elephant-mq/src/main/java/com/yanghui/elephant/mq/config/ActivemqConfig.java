package com.yanghui.elephant.mq.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.StringUtils;

import com.yanghui.elephant.mq.producer.ProducerService;
import com.yanghui.elephant.mq.producer.impl.ActivemqProducerService;
import com.yanghui.elephant.mq.properties.MQProperties;

@Configuration
public class ActivemqConfig {
	
	@Autowired
	private MQProperties mqProperties;
	
	@Bean
	@ConditionalOnProperty(prefix="elephant.mq",value="activemq-broker-url")
	public SingleConnectionFactory singleConnectionFactory(){
		ActiveMQConnectionFactory targetConnectionFactory = new ActiveMQConnectionFactory(this.mqProperties.getActivemqBrokerUrl());
		if(!StringUtils.isEmpty(this.mqProperties.getActivemqUserName()) && 
				!StringUtils.isEmpty(this.mqProperties.getActivemqPassword())){
			targetConnectionFactory.setUserName(this.mqProperties.getActivemqUserName());
			targetConnectionFactory.setPassword(this.mqProperties.getActivemqPassword());
		}else {
			targetConnectionFactory.setUserName(ActiveMQConnectionFactory.DEFAULT_USER);
			targetConnectionFactory.setPassword(ActiveMQConnectionFactory.DEFAULT_PASSWORD);
		}
		
		PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
		pooledConnectionFactory.setConnectionFactory(targetConnectionFactory);
		pooledConnectionFactory.setMaxConnections(this.mqProperties.getActivemqPoolMaxConnections());
		
		SingleConnectionFactory singleConnectionFactory = new SingleConnectionFactory();
		singleConnectionFactory.setTargetConnectionFactory(pooledConnectionFactory);
		return singleConnectionFactory;
	}
	
	@Bean
	@ConditionalOnBean(SingleConnectionFactory.class)
	public JmsTemplate jmsTemplate(SingleConnectionFactory singleConnectionFactory){
		JmsTemplate jmsTemplate = new JmsTemplate();
		jmsTemplate.setConnectionFactory(singleConnectionFactory);
		return jmsTemplate;
	}
	
	@Bean
	@ConditionalOnBean(JmsTemplate.class)
	public ProducerService activemqProducerService(JmsTemplate jmsTemplate){
		ActivemqProducerService activemqProducer = new ActivemqProducerService();
		activemqProducer.setJmsTemplate(jmsTemplate);
		return activemqProducer;
	}
}
