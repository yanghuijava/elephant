package com.yanghui.elephant.client.impl;

import io.netty.util.internal.StringUtil;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j2;

import com.alibaba.fastjson.JSON;
import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.client.producer.DefaultMQProducer;
import com.yanghui.elephant.client.producer.LocalTransactionExecuter;
import com.yanghui.elephant.client.producer.SendResult;
import com.yanghui.elephant.client.producer.SendStatus;
import com.yanghui.elephant.client.producer.TransactionMQProducer;
import com.yanghui.elephant.client.producer.TransactionSendResult;
import com.yanghui.elephant.common.constant.RequestCode;
import com.yanghui.elephant.common.constant.ResponseCode;
import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.common.message.MessageConstant;
import com.yanghui.elephant.common.message.MessageType;
import com.yanghui.elephant.common.protocol.header.CheckTransactionStateResponseHeader;
import com.yanghui.elephant.common.protocol.header.EndTransactionRequestHeader;
import com.yanghui.elephant.common.protocol.header.SendMessageRequestHeader;
import com.yanghui.elephant.remoting.exception.RemotingConnectException;
import com.yanghui.elephant.remoting.exception.RemotingSendRequestException;
import com.yanghui.elephant.remoting.exception.RemotingTimeoutException;
import com.yanghui.elephant.remoting.netty.NettyClientConfig;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
@Log4j2
public class DefaultMQProducerImpl implements MQProducerInner{

	protected MQClientInstance mqProducerFactory;

	protected DefaultMQProducer defaultMQProducer;

	protected BlockingQueue<Runnable> checkRequestQueue;
	
	protected ExecutorService checkExecutor;
	

	public DefaultMQProducerImpl(DefaultMQProducer defaultMQProducer) {
		this.mqProducerFactory = MQClientInstance.getInstance();
		this.defaultMQProducer = defaultMQProducer;
	}

	public void start() throws MQClientException{
		this.checkConfig();
		this.mqProducerFactory.setNettyClientConfig(new NettyClientConfig());
		this.mqProducerFactory.setRegisterCenter(this.defaultMQProducer.getRegisterCenter());
		this.mqProducerFactory.start();
	}
	
	private void checkConfig() throws MQClientException {
        if (StringUtil.isNullOrEmpty(this.defaultMQProducer.getProducerGroup())) {
            throw new MQClientException("producerGroup is null", null);
        }
        if(StringUtil.isNullOrEmpty(this.defaultMQProducer.getProducerGroup())){
        	throw new MQClientException("register center is null", null);
        }
        synchronized (this.mqProducerFactory) {
        	if(this.mqProducerFactory.getProducerMap().get(this.defaultMQProducer.getProducerGroup()) != null){
        		throw new MQClientException("Group can not be the same！", null);
        	}
        	this.mqProducerFactory.getProducerMap().put(this.defaultMQProducer.getProducerGroup(), this);
		}
        
    }
	public void shutdown() {
		this.mqProducerFactory.shutdown();
	}

	public SendResult send(Message msg) throws MQClientException{
		checkMessage(msg, defaultMQProducer);
		SendResult result = new SendResult();
		
		SendMessageRequestHeader requestHeader = new SendMessageRequestHeader();
		requestHeader.setDestination(msg.getDestination());
		requestHeader.setMessageId(msg.getMessageId());
		requestHeader.setProducerGroup(this.defaultMQProducer.getProducerGroup());
		requestHeader.setProperties(JSON.toJSONString(msg.getProperties()));
		
		String MessageTypeName = msg.getProperties().get(MessageConstant.PROPERTY_TRANSACTION_PREPARED);
		requestHeader.setMessageType(MessageType.valueOfName(MessageTypeName).getType());
		
		RemotingCommand request = RemotingCommand.buildRequestCmd(requestHeader, RequestCode.SEND_MESSAGE);
		request.setBody(msg.getBody());
		
		MQClientException exception = null;
		int timesTotal = 1 + this.defaultMQProducer.getRetryTimesWhenSendFailed();
		result.setMsgId(msg.getMessageId());
		for (int times=0; times < timesTotal; times++) {
			try {
				RemotingCommand response = this.mqProducerFactory.getRemotingClient()
						.invokeSync(choiceOneServer(), request, this.defaultMQProducer.getSendMsgTimeout());
				switch (response.getCode()) {
				case ResponseCode.SUCCESS:
					result.setSendStatus(SendStatus.SEND_OK);
					break;
				case ResponseCode.FUSH_DB_FAIL:
					result.setSendStatus(SendStatus.FLUSH_DB_FAIL);
					break;
				case ResponseCode.SEND_MQ_FAIL:
					result.setSendStatus(SendStatus.SEND_MQ_FAIL);
					break;
				case ResponseCode.SERVER_FAIL:
					result.setSendStatus(SendStatus.SERVER_FAIL);
					break;
				default:
					result.setSendStatus(SendStatus.SEND_FAIL);
					break;
				}
				return result;
			} catch(RemotingTimeoutException e){
				result.setSendStatus(SendStatus.FLUSH_DISK_TIMEOUT);
				continue;
			}catch (Exception e) {
				exception =  new MQClientException("message send exception", e);
				continue;
			}
		}
		if(exception != null) {
			throw exception;
		}
		return result;
	}
	
	private String choiceOneServer(){
		List<String> servers = this.mqProducerFactory.getServers();
		return servers.get(new Random().nextInt(servers.size()));
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
            this.checkRequestQueue,
            new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "transactioncheckExecutorThread");
				}
			});
       this.mqProducerFactory.registerDefaultRequestProcessor();
	}
	
	@Override
	public void checkTransactionState(final String address,final String producerGroupe,final Message msg) {
		Runnable run = new Runnable() {
			@Override
			public void run() {
				try {
					TransactionMQProducer producer = (TransactionMQProducer) defaultMQProducer;
					LocalTransactionState localState = producer.getTransactionCheckListener().checkLocalTransactionState(msg);
					
					CheckTransactionStateResponseHeader header = new CheckTransactionStateResponseHeader();
					header.setCommitOrRollback(localState.name());
					header.setMessageId(msg.getMessageId());
					header.setProducerGroup(defaultMQProducer.getProducerGroup());
					
					RemotingCommand request = RemotingCommand.buildRequestCmd(header,RequestCode.CHECK_TRANSACTION_RESPONSE);
					mqProducerFactory.getRemotingClient().invokeOneway(address, request, 0);
				} catch (Exception e){
					log.error("check transaction state error:{}",e);
				}
			}
		};
		this.checkExecutor.submit(run);
	}
	
	public void destroyTransaction() {
        this.checkExecutor.shutdown();
        this.checkRequestQueue.clear();
    }
	
	public TransactionSendResult sendMessageInTransaction(final Message msg,final LocalTransactionExecuter tranExecuter, final Object arg)throws MQClientException{
		TransactionSendResult transactionSendResult = new TransactionSendResult();
		SendResult sendResult = null;
		
		msg.getProperties().put(MessageConstant.PROPERTY_TRANSACTION_PREPARED, 
				MessageType.TRANSACTION_PRE_MESSAGE.name());
		
		try {
			sendResult = this.send(msg);
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
		EndTransactionRequestHeader requestHeader = new EndTransactionRequestHeader();
		requestHeader.setMsgId(sendResult.getMsgId());
		requestHeader.setProducerGroup(this.defaultMQProducer.getProducerGroup());
		requestHeader.setCommitOrRollback(localState.name());
		
		RemotingCommand request = RemotingCommand.buildRequestCmd(requestHeader,RequestCode.END_TRANSACTION);
		
		if(localException != null){
			request.setRemark("executeLocalTransactionBranch exception: " + localException.toString());
		}
		this.mqProducerFactory.getRemotingClient().invokeOneway(choiceOneServer(), request, this.defaultMQProducer.getSendMsgTimeout());
	}
}
