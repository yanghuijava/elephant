package com.yanghui.elephant.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yanghui.elephant.register.impl.ZkClientRegisterCenter;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.netty.NettyRemotingServer;
import com.yanghui.elephant.remoting.netty.NettyServerConfig;

@Configuration
public class ServerConfiguration {
	
	@Autowired
	private RequestProcessor requestProcessor;
	
	@Bean(initMethod="start",destroyMethod="shutdown")
	public NettyRemotingServer nettyRemotingServer(){
		NettyRemotingServer server = new NettyRemotingServer(new NettyServerConfig());
		server.registerDefaultProcessor(this.requestProcessor, null);
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
