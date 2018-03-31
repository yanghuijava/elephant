package com.yanghui.elephant.example.quickstart;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.producer.DefaultMQProducer;
import com.yanghui.elephant.client.producer.SendResult;
import com.yanghui.elephant.common.message.Message;

public class Producer {

	public static void main(String[] args) throws MQClientException {
		DefaultMQProducer producer = new DefaultMQProducer("test");
		producer.setRegisterCenter("120.77.152.143:2181");
		producer.start();
		/**
		 * 目前只支持activemq：
		 * 发送queue，message的destination值加上前缀：queue://
		 * 发送topic，message的destination值加上前缀：topic://
		 */
		try {
			for(int i=0;i<1;i++){
				Message msg = new Message("queue://yanghui.queue.test1", ("我是消息" + i).getBytes());
				SendResult sendResult = producer.send(msg);
				System.out.println(sendResult);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		producer.shutdown();
	}
}
