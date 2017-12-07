package com.yanghui.elephant.client.impl;

import io.netty.util.internal.StringUtil;

import java.util.List;
import java.util.Random;
import com.google.common.collect.Lists;
import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.producer.DefaultMQProducer;
import com.yanghui.elephant.client.producer.SendResult;
import com.yanghui.elephant.client.producer.SendStatus;
import com.yanghui.elephant.common.constant.Constant;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.register.dto.ServerDto;
import com.yanghui.elephant.register.listener.IServerChanngeListener;
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

	public void start() throws MQClientException{
		this.checkConfig();
		this.mqProducerFactory.setNettyClientConfig(new NettyClientConfig());
		this.mqProducerFactory.setRegisterCenter(this.defaultMQProducer.getRegisterCenter());
		this.mqProducerFactory.start();
		initServers();
	}
	
	private void checkConfig() throws MQClientException {
        if (StringUtil.isNullOrEmpty(this.defaultMQProducer.getProducerGroup())) {
            throw new MQClientException("producerGroup is null", null);
        }
        if(StringUtil.isNullOrEmpty(this.defaultMQProducer.getProducerGroup())){
        	throw new MQClientException("register center is null", null);
        }
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
		this.mqProducerFactory.getRegisterCenter4Invoker().registerServerChanngeListener(this);
	}

	public void shutdown() {
		this.mqProducerFactory.shutdown();
	}

	@Override
	public void handleServerChannge(List<ServerDto> serverDtoList) {
		this.servers.clear();
		List<String> newServers = Lists.newArrayList();
		for (ServerDto dto : serverDtoList) {
			newServers.add(dto.getIp() + ":" + dto.getPort());
		}
		this.servers.addAll(newServers);
	}

	public SendResult send(Message msg,CommandCustomHeader header) throws MQClientException{
		checkMessage(msg, defaultMQProducer);
		SendResult result = new SendResult();
		RemotingCommand request = RemotingCommand.buildRequestCmd(msg, header);
		try {
			RemotingCommand response = this.mqProducerFactory.getRemotingClient()
					.invokeSync(choiceOneServer(), request, this.defaultMQProducer.getSendMsgTimeout());
			switch (response.getCode()) {
			case Constant.SUCCESS:
				result.setMsgId(msg.getMessageId());
				result.setSendStatus(SendStatus.SEND_OK);
				break;
			case Constant.FUSH_DB_FAIL:
				result.setSendStatus(SendStatus.FLUSH_DB_FAIL);
				break;
			case Constant.SEND_MQ_FAIL:
				result.setSendStatus(SendStatus.SEND_MQ_FAIL);
				break;
			case Constant.SERVER_FAIL:
				result.setSendStatus(SendStatus.SERVER_FAIL);
				break;
			default:
				result.setSendStatus(SendStatus.SEND_FAIL);
				break;
			}
		} catch(RemotingTimeoutException e){
			result.setSendStatus(SendStatus.FLUSH_DISK_TIMEOUT);
		}catch (Exception e) {
			throw new MQClientException("message send exception", e);
		}
		return result;
	}
	
	private String choiceOneServer(){
		return this.servers.get(new Random().nextInt(this.servers.size()));
	}
	
	public static void checkMessage(Message msg,DefaultMQProducer defaultMQProducer) throws MQClientException {
		if (null == msg) {
			throw new MQClientException(1,"the message is null");
		}
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
