package com.yanghui.elephant.client.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.StringUtil;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j2;

import com.google.common.collect.Lists;
import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.producer.DefaultMQProducer;
import com.yanghui.elephant.client.producer.LocalTransactionExecuter;
import com.yanghui.elephant.client.producer.SendResult;
import com.yanghui.elephant.client.producer.SendStatus;
import com.yanghui.elephant.client.producer.TransactionMQProducer;
import com.yanghui.elephant.client.producer.TransactionSendResult;
import com.yanghui.elephant.common.constant.Constant;
import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.constant.RemotingCommandCode;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.register.dto.ServerDto;
import com.yanghui.elephant.register.listener.IServerChanngeListener;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.exception.RemotingConnectException;
import com.yanghui.elephant.remoting.exception.RemotingSendRequestException;
import com.yanghui.elephant.remoting.exception.RemotingTimeoutException;
import com.yanghui.elephant.remoting.netty.NettyClientConfig;
import com.yanghui.elephant.remoting.procotol.CommandCustomHeader;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import com.yanghui.elephant.remoting.procotol.header.MessageRequestHeader;
@Log4j2
public class DefaultMQProducerImpl implements IServerChanngeListener {

	protected MQProducerInstance mqProducerFactory;

	protected DefaultMQProducer defaultMQProducer;

	protected volatile List<String> servers;
	
	protected BlockingQueue<Runnable> checkRequestQueue;
	
	protected ExecutorService checkExecutor;

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

	public SendResult send(Message msg,CommandCustomHeader header,RemotingCommandCode remotingCommandCode) throws MQClientException{
		checkMessage(msg, defaultMQProducer);
		SendResult result = new SendResult();
		RemotingCommand request = RemotingCommand.buildRequestCmd(msg, header,remotingCommandCode);
		try {
			RemotingCommand response = this.mqProducerFactory.getRemotingClient()
					.invokeSync(choiceOneServer(), request, this.defaultMQProducer.getSendMsgTimeout());
			switch (response.getCode()) {
			case Constant.SUCCESS:
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
		result.setMsgId(msg.getMessageId());
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
		if(StringUtil.isNullOrEmpty(msg.getMessageId())){
			msg.setMessageId(UUID.randomUUID().toString());
		}
	}

	public void initTransaction() {
		TransactionMQProducer producer = (TransactionMQProducer) this.defaultMQProducer;
        this.checkRequestQueue = new LinkedBlockingQueue<Runnable>(producer.getCheckRequestHoldMax());
        this.checkExecutor = new ThreadPoolExecutor(
            producer.getCheckThreadPoolMinSize(),
            producer.getCheckThreadPoolMaxSize(),
            1000 * 60,
            TimeUnit.MILLISECONDS,
            this.checkRequestQueue);
        this.mqProducerFactory.getRemotingClient().registerDefaultProcessor(
        		new CheckLocalTransactionStateRequestProcessor(), this.checkExecutor);
	}
	
	class CheckLocalTransactionStateRequestProcessor implements RequestProcessor{
		@Override
		public RemotingCommand processRequest(ChannelHandlerContext ctx,RemotingCommand request) {
			TransactionMQProducer producer = (TransactionMQProducer) defaultMQProducer;
			Message msg = (Message)request.getBody();
			LocalTransactionState localState = producer.getTransactionCheckListener().checkLocalTransactionState(msg);
			MessageRequestHeader customHeader = new MessageRequestHeader();
			customHeader.setGroup(defaultMQProducer.getProducerGroup());
			customHeader.setLocalTransactionState(localState);
			RemotingCommand checkTransactionRequest = RemotingCommand.buildRequestCmd(msg.getMessageId(),
					customHeader,RemotingCommandCode.TRANSACTION_CHECK_MESSAGE);
			ctx.writeAndFlush(checkTransactionRequest);
			return null;
		}
	}
	
	public void destroyTransaction() {
        this.checkExecutor.shutdown();
        this.checkRequestQueue.clear();
    }
	
	public TransactionSendResult sendMessageInTransaction(final Message msg,CommandCustomHeader header,final LocalTransactionExecuter tranExecuter, final Object arg)throws MQClientException{
		TransactionSendResult transactionSendResult = new TransactionSendResult();
		SendResult sendResult = null;
		try {
			sendResult = this.send(msg, header,RemotingCommandCode.TRANSACTION_PRE_MESSAGE);
		} catch (Exception e) {
			throw new MQClientException("send message Exception", e);
		}
		Throwable localException = null;
		LocalTransactionState localState = LocalTransactionState.UNKNOW;
		switch (sendResult.getSendStatus()) {
		case SEND_OK:
			try {
				localState = tranExecuter.executeLocalTransactionBranch(msg, arg);
				if(null == localState){
					localState = LocalTransactionState.UNKNOW;
				}
				if(localState != LocalTransactionState.COMMIT_MESSAGE){
					log.info("executeLocalTransactionBranch return {}", localState);
                    log.info(msg.toString());
				}
			} catch (Throwable e) {
				localException = e;
				log.info("executeLocalTransactionBranch exception", e);
                log.info(msg.toString());
			}
			break;
		default:
			localState = LocalTransactionState.ROLLBACK_MESSAGE;
			break;
		}
		
		try {
			this.endTransaction(sendResult,localState,localException);
		} catch (Exception e) {
			log.warn("local transaction execute " + localState + ", but end broker transaction failed", e);
		}
		transactionSendResult.setMsgId(sendResult.getMsgId());
		transactionSendResult.setSendStatus(sendResult.getSendStatus());
		transactionSendResult.setLocalTransactionState(localState);
		return transactionSendResult;
	}

	private void endTransaction(SendResult sendResult,LocalTransactionState localState,
			Throwable localException) throws InterruptedException, RemotingSendRequestException,RemotingTimeoutException,RemotingConnectException{
		MessageRequestHeader customHeader = new MessageRequestHeader();
		customHeader.setGroup(this.defaultMQProducer.getProducerGroup());
		customHeader.setLocalTransactionState(localState);
		RemotingCommand request = RemotingCommand.buildRequestCmd(sendResult.getMsgId(), customHeader, RemotingCommandCode.TRANSACTION_END_MESSAGE);
		if(localException != null){
			request.setRemark("executeLocalTransactionBranch exception: " + localException.toString());
		}
		this.mqProducerFactory.getRemotingClient().invokeOneway(choiceOneServer(), request, this.defaultMQProducer.getSendMsgTimeout());
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
}
