package com.yanghui.elephant.example.quickstart;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.producer.DefaultMQProducer;
import com.yanghui.elephant.client.producer.SendResult;
import com.yanghui.elephant.common.message.Message;

public class Producer {

	public static void main(String[] args) throws MQClientException {
		DefaultMQProducer producer = new DefaultMQProducer("test");
		producer.setRegisterCenter("172.16.21.12:2181");
		producer.start();
		
		for(int i=0;i<1;i++){
			Message msg = new Message("topic://VirtualTopic.Test", ("我是消息" + i).getBytes());
			SendResult sendResult = producer.send(msg);
			System.out.println(sendResult);
		}
//		producer.shutdown();
	}
}
