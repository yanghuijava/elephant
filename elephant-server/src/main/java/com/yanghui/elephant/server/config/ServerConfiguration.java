package com.yanghui.elephant.server.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.yanghui.elephant.common.constant.RequestCode;
import com.yanghui.elephant.register.impl.ZkClientRegisterCenter;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.netty.NettyRemotingServer;
import com.yanghui.elephant.remoting.netty.NettyServerConfig;

@Configuration
public class ServerConfiguration {
	
	@Resource(name="messageRequestProcessor")
	private RequestProcessor messageRequestProcessor;
	@Resource(name="endTransactionMessageProcessor")
	private RequestProcessor endTransactionMessageProcessor;
	@Resource(name="checkTransactionStateResponseProcessor")
	private RequestProcessor checkTransactionStateResponseProcessor;
	@Resource(name="heartbeatRequestProcessor")
	private RequestProcessor heartbeatRequestProcessor;
	
	private ExecutorService heartbeatExecutor;
	
	private ExecutorService handleMessageExecutor;
	
	private ExecutorService endTransactionMessageExecutor;
	
	private ExecutorService checkTransactionStateResponseExecutor;
	
	@PostConstruct
    public void initMethod() {
		ThreadFactory threadFactory1 = new ThreadFactoryBuilder()
	        .setNameFormat("heart-beat-executor-%d")
	        .setDaemon(true)
	        .build();
        this.heartbeatExecutor = Executors.newFixedThreadPool(
        		Runtime.getRuntime().availableProcessors(),threadFactory1);
    
        ThreadFactory threadFactory2 = new ThreadFactoryBuilder()
        		.setNameFormat("handle-message-executor-%d")
    	        .setDaemon(true)
    	        .build();
        this.handleMessageExecutor = Executors.newFixedThreadPool(
        		Runtime.getRuntime().availableProcessors(),threadFactory2);
        
        ThreadFactory threadFactory3 = new ThreadFactoryBuilder()
        		.setNameFormat("end-transaction-message-executor-%d")
    	        .setDaemon(true)
    	        .build();
        this.endTransactionMessageExecutor = Executors.newFixedThreadPool(
        		Runtime.getRuntime().availableProcessors(),threadFactory3);
        
        ThreadFactory threadFactory4 = new ThreadFactoryBuilder()
        		.setNameFormat("check-transactionState-response-executor-%d")
    	        .setDaemon(true)
    	        .build();
        this.checkTransactionStateResponseExecutor = Executors.newFixedThreadPool(
        		Runtime.getRuntime().availableProcessors(),threadFactory4);
	}
	
	@PreDestroy  
    public void destroyMethod() {  
        this.heartbeatExecutor.shutdown();
    }  
	
	@Bean(initMethod="start",destroyMethod="shutdown")
	public NettyRemotingServer nettyRemotingServer(){
		NettyRemotingServer server = new NettyRemotingServer(new NettyServerConfig());
		server.registerProcessor(RequestCode.SEND_MESSAGE,this.messageRequestProcessor,this.handleMessageExecutor);
		server.registerProcessor(RequestCode.END_TRANSACTION,this.endTransactionMessageProcessor,
				this.endTransactionMessageExecutor);
		server.registerProcessor(RequestCode.HEART_BEAT, this.heartbeatRequestProcessor, this.heartbeatExecutor);
		server.registerProcessor(RequestCode.CHECK_TRANSACTION_RESPONSE, this.checkTransactionStateResponseProcessor,
				this.checkTransactionStateResponseExecutor);
		return server;
	}
	
	@Bean(initMethod="init",destroyMethod="destroy")
	@ConditionalOnProperty(prefix = "elephant",value = {"zk-server"})
	public ZkClientRegisterCenter zkClientRegisterCenter(@Value("${elephant.zk-server}")String zkServer){
		ZkClientRegisterCenter registerCenter = new ZkClientRegisterCenter();
		registerCenter.setZkAddress(zkServer);
		return registerCenter;
	}
}
