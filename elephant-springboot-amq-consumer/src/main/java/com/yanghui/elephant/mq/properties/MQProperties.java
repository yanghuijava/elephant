package com.yanghui.elephant.mq.properties;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="elephant.mq")
@Data
public class MQProperties {
	
	private String activemqBrokerUrl;
	
	private String activemqUserName;
	
	private String activemqPassword;
	
	private int activemqPoolMaxConnections = 10;
	
	private long redeliveryDelay = 10000L;

}
