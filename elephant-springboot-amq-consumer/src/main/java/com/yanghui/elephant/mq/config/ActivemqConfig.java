package com.yanghui.elephant.mq.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.StringUtils;

import com.yanghui.elephant.mq.properties.MQProperties;

@Configuration
public class ActivemqConfig {
	
	@Autowired
	private MQProperties mqProperties;
	
	@Bean
    public RedeliveryPolicy redeliveryPolicy() {
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setInitialRedeliveryDelay(5000);
        redeliveryPolicy.setRedeliveryDelay(this.mqProperties.getRedeliveryDelay());
        redeliveryPolicy.setBackOffMultiplier(1);
        redeliveryPolicy.setUseExponentialBackOff(true);
        redeliveryPolicy.setMaximumRedeliveries(-1);
        return redeliveryPolicy;
    }
	
	@Bean
	public SingleConnectionFactory singleConnectionFactory(RedeliveryPolicy redeliveryPolicy){
		ActiveMQConnectionFactory targetConnectionFactory = new ActiveMQConnectionFactory(this.mqProperties.getActivemqBrokerUrl());
		if(!StringUtils.isEmpty(this.mqProperties.getActivemqUserName()) && 
				!StringUtils.isEmpty(this.mqProperties.getActivemqPassword())){
			targetConnectionFactory.setUserName(this.mqProperties.getActivemqUserName());
			targetConnectionFactory.setPassword(this.mqProperties.getActivemqPassword());
		}else {
			targetConnectionFactory.setUserName(ActiveMQConnectionFactory.DEFAULT_USER);
			targetConnectionFactory.setPassword(ActiveMQConnectionFactory.DEFAULT_PASSWORD);
		}
		targetConnectionFactory.setRedeliveryPolicy(redeliveryPolicy);
		
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
}
