package com.yanghui.elephant.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
