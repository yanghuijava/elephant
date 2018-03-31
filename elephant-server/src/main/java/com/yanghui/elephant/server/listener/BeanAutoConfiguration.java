package com.yanghui.elephant.server.listener;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.yanghui.elephant.common.utils.DateUtil;
import com.yanghui.elephant.common.utils.IPHelper;
import com.yanghui.elephant.register.IRegisterCenter4Provider;
import com.yanghui.elephant.register.dto.ServerDto;
import com.yanghui.elephant.register.listener.IZKReconnectionListener;
import com.yanghui.elephant.remoting.RemotingServer;

@Component
public class BeanAutoConfiguration implements ApplicationListener<EmbeddedServletContainerInitializedEvent>,IZKReconnectionListener{

	@Autowired
	private IRegisterCenter4Provider registerCenter4Provider;
	@Autowired
	private RemotingServer remotingServer;
	@Value("${spring.application.name}")
	private String serverName;
	@Value("${elephant.server.public-network-ip}")
	private String publicNetworkIp;
	
	private volatile Boolean flag = false;
	
	@Override
	public void handleStateForSyncConnected() {
		if(!this.flag){
			this.registerCenter4Provider.addZKReconnectionListener(this);
			this.registerCenter4Provider.registerServer(buildServerDto());
			this.flag = true;
		}
	}

	@Override
	public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
		this.registerCenter4Provider.registerServer(buildServerDto());
	}
	
	private ServerDto buildServerDto(){
		ServerDto serverDto = new ServerDto();
		serverDto.setServerName(this.serverName);
		
		serverDto.setPort(this.remotingServer.localListenPort());
		serverDto.setDateTime(DateUtil.formatAsDateTime(new Date()));
		
		if(!StringUtils.isEmpty(this.publicNetworkIp)) {
			serverDto.setIp(this.publicNetworkIp);
		}else {
			serverDto.setIp(IPHelper.getRealIp());
		}
		return serverDto;
	}

}
