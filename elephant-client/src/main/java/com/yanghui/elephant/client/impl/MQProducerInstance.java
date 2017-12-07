package com.yanghui.elephant.client.impl;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import com.yanghui.elephant.register.IRegisterCenter4Invoker;
import com.yanghui.elephant.register.IRegisterCenter4Provider;
import com.yanghui.elephant.register.impl.ZkClientRegisterCenter;
import com.yanghui.elephant.remoting.RemotingClient;
import com.yanghui.elephant.remoting.netty.NettyClientConfig;
import com.yanghui.elephant.remoting.netty.NettyRemotingClient;

@Data
public class MQProducerInstance {
	
	private static final MQProducerInstance instance = new MQProducerInstance();
	
	private String registerCenter;
	private NettyClientConfig nettyClientConfig;
	private RemotingClient remotingClient;
	private IRegisterCenter4Invoker registerCenter4Invoker;
	private IRegisterCenter4Provider registerCenter4Provider;
	
	@Setter(value=AccessLevel.PRIVATE)
	private volatile boolean started = false;
	@Setter(value=AccessLevel.PRIVATE)
	private volatile boolean shutdown = false;
	
	private MQProducerInstance(){
	}
	
	public static MQProducerInstance getInstance(){
		return instance;
	}
	
	public synchronized void start(){
		if(this.started){
			return;
		}
		this.remotingClient = new NettyRemotingClient(this.nettyClientConfig);
		this.remotingClient.start();
		ZkClientRegisterCenter zkClientRegisterCenter = new ZkClientRegisterCenter();
		zkClientRegisterCenter.setZkAddress(this.registerCenter);
		this.registerCenter4Invoker = zkClientRegisterCenter;
		this.registerCenter4Provider = zkClientRegisterCenter;
		zkClientRegisterCenter.init();
		this.started = true;
	}
	
	public synchronized void shutdown(){
		if(this.shutdown){
			return;
		}
		this.remotingClient.shutdown();
		this.registerCenter4Provider.destroy();
		this.shutdown = true;
	}
}
