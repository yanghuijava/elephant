package com.yanghui.elephant.client.impl;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.producer.DefaultMQProducer;
import com.yanghui.elephant.client.producer.SendResult;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.register.dto.ServerDto;
import com.yanghui.elephant.register.listener.IServerChanngeListener;
import com.yanghui.elephant.remoting.exception.RemotingConnectException;
import com.yanghui.elephant.remoting.exception.RemotingSendRequestException;
import com.yanghui.elephant.remoting.exception.RemotingTimeoutException;
import com.yanghui.elephant.remoting.netty.NettyClientConfig;
import com.yanghui.elephant.remoting.procotol.CommandCustomHeader;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;

public class DefaultMQProducerImpl implements IServerChanngeListener {

	private MQProducerInstance mqProducerFactory;

	private DefaultMQProducer defaultMQProducer;

	private volatile List<String> servers;

	public DefaultMQProducerImpl(DefaultMQProducer defaultMQProducer) {
		this.mqProducerFactory = MQProducerInstance.getInstance();
		this.defaultMQProducer = defaultMQProducer;
	}

	public void start() {
		this.mqProducerFactory.setNettyClientConfig(new NettyClientConfig());
		this.mqProducerFactory.setRegisterCenter(this.defaultMQProducer
				.getRegisterCenter());
		this.mqProducerFactory.start();
		initServers();
	}

	private void initServers() {
		/**
		 * 获取服务器地址ip:port
		 */
		List<ServerDto> serverDtoList = this.mqProducerFactory
				.getRegisterCenter4Invoker().getServerList();
		if (serverDtoList.isEmpty()) {
			throw new RuntimeException("server not start!");
		}
		this.servers = Lists.newArrayList();
		for (ServerDto dto : serverDtoList) {
			servers.add(dto.getIp() + ":" + dto.getPort());
		}
		/**
		 * 注册服务器变化监听器
		 */
		this.mqProducerFactory.getRegisterCenter4Invoker()
				.registerServerChanngeListener(this);
	}

	public void shutdown() {
		this.mqProducerFactory.shutdown();
	}

	@Override
	public void handleServerChannge(List<ServerDto> serverDtoList) {
		this.servers.clear();
		for (ServerDto dto : serverDtoList) {
			this.servers.add(dto.getIp() + ":" + dto.getPort());
		}
	}

	public SendResult send(Message msg,CommandCustomHeader header) throws MQClientException{
		checkMessage(msg, defaultMQProducer);
		SendResult result = null;
		RemotingCommand request = RemotingCommand.buildRequestCmd(msg, header);
		try {
			RemotingCommand response = this.mqProducerFactory.getRemotingClient()
					.invokeSync(choice(), request, this.defaultMQProducer.getSendMsgTimeout());
			
		} catch (Exception e) {
			throw new MQClientException("message send exception", e);
		}
		return result;
	}
	
	private String choice(){
		return this.servers.get(new Random().nextInt(this.servers.size()));
	}
	
	public static void checkMessage(Message msg,DefaultMQProducer defaultMQProducer) throws MQClientException {
		if (null == msg) {
			throw new MQClientException(1,"the message is null");
		}
		// body
		if (null == msg.getBody()) {
			throw new MQClientException(2,"the message body is null");
		}

		if (0 == msg.getBody().length) {
			throw new MQClientException(3,"the message body length is zero");
		}
		if (msg.getBody().length > defaultMQProducer.getMaxMessageSize()) {
			throw new MQClientException(4,"the message body size over max value, MAX: " + defaultMQProducer.getMaxMessageSize());
		}
	}
}
