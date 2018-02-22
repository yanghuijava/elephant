package com.yanghui.elephant.mq.config;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.yanghui.elephant.mq.annotation.EnableElephantMQ;
import com.yanghui.elephant.mq.annotation.QueueListener;
import com.yanghui.elephant.mq.annotation.TopicListnener;
import com.yanghui.elephant.mq.constant.MQConstant;
import com.yanghui.elephant.mq.listener.SessionAwareMessageListenerImpl;
import com.yanghui.elephant.mq.properties.MQProperties;

@Configuration
@ConditionalOnBean(annotation={EnableElephantMQ.class})
@Import({MQProperties.class,ActivemqConfig.class})
public class MQAutoConfiguration implements ApplicationListener<ContextRefreshedEvent>{
	
	private volatile boolean isRepeat = false;
	@Autowired
	private ConnectionFactory connectionFactory;
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private JmsTemplate jmsTemplate;
	
	private static AtomicInteger count = new AtomicInteger();
	
	@Override
	public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
		if(isRepeat){
			return;
		}
		this.isRepeat = true;
		this.applicationContext = event.getApplicationContext();
		for(String beanName : this.applicationContext.getBeanDefinitionNames()){
			Class<?> clz = this.applicationContext.getType(beanName);
			if(clz == null) continue;
			Method[] methods = ReflectionUtils.getAllDeclaredMethods(clz);
			if(methods == null || methods.length <= 0){
				continue;
			}
			for(Method method : methods){
				QueueListener queueListener = AnnotationUtils.findAnnotation(method, QueueListener.class);
				String registerBeanName = null;
				String retryRegisterBeanName = null;
				if(queueListener != null){
					registerBeanName = "queueConsumer" + count.getAndIncrement();
					this.registerListener(
							registerBeanName, 
							new ActiveMQQueue(queueListener.name()),
							this.applicationContext.getBean(beanName),
							method,
							queueListener.retryTimes(),
							false);
					retryRegisterBeanName = "retryQueueConsumer" + count.getAndIncrement();
					this.registerListener(
							retryRegisterBeanName, 
							new ActiveMQQueue(queueListener.name() + MQConstant.RETRY_QUEUE_SUFFIX),
							this.applicationContext.getBean(beanName),
							method,
							0,
							true);
				}
				TopicListnener topicListnener = AnnotationUtils.findAnnotation(method, TopicListnener.class);
				if(topicListnener != null){
					registerBeanName = "topicConsumer" + count.getAndIncrement();
					this.registerListener(
							registerBeanName, 
							new ActiveMQTopic(topicListnener.name()), 
							this.applicationContext.getBean(beanName),
							method,
							topicListnener.retryTimes(),
							false);
					retryRegisterBeanName = "retryTopicConsumer" + count.getAndIncrement();
					this.registerListener(
							retryRegisterBeanName, 
							new ActiveMQTopic(topicListnener.name() + MQConstant.RETRY_QUEUE_SUFFIX),
							this.applicationContext.getBean(beanName),
							method,
							0,
							true);
				}
				start(registerBeanName);
				start(retryRegisterBeanName);
			}
		}
	}
	
	private void start(String beanName){
		if(StringUtils.isEmpty(beanName)){
			return;
		}
		DefaultMessageListenerContainer c = this.applicationContext.
				getBean(beanName,DefaultMessageListenerContainer.class);
		if(c != null)c.start();
	}
	
	private void registerListener(String registerBeanName,Destination destination,Object bean,Method method,int retryTimes,boolean isRetry){
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)this.applicationContext.getAutowireCapableBeanFactory();  
          
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultMessageListenerContainer.class);  
        beanDefinitionBuilder.addPropertyValue("connectionFactory",this.connectionFactory);  
        beanDefinitionBuilder.addPropertyValue("destination", destination);  
        beanDefinitionBuilder.addPropertyValue("messageListener",new SessionAwareMessageListenerImpl(bean, method,retryTimes,isRetry,this.jmsTemplate));
        beanDefinitionBuilder.addPropertyValue("sessionAcknowledgeMode",4);
        
        beanFactory.registerBeanDefinition(registerBeanName, beanDefinitionBuilder.getBeanDefinition());
	}
}
