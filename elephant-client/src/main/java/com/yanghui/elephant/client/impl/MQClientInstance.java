package com.yanghui.elephant.client.impl;

import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import com.google.common.collect.Lists;
import com.yanghui.elephant.common.constant.RequestCode;
import com.yanghui.elephant.register.IRegisterCenter4Invoker;
import com.yanghui.elephant.register.dto.ServerDto;
import com.yanghui.elephant.register.impl.ZkClientRegisterCenter;
import com.yanghui.elephant.register.listener.IServerChanngeListener;
import com.yanghui.elephant.remoting.RemotingClient;
import com.yanghui.elephant.remoting.RequestProcessor;
import com.yanghui.elephant.remoting.common.RemotingHelper;
import com.yanghui.elephant.remoting.exception.RemotingConnectException;
import com.yanghui.elephant.remoting.exception.RemotingSendRequestException;
import com.yanghui.elephant.remoting.exception.RemotingTimeoutException;
import com.yanghui.elephant.remoting.netty.NettyClientConfig;
import com.yanghui.elephant.remoting.netty.NettyRemotingClient;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;

@Data
@Log4j2
public class MQClientInstance implements IServerChanngeListener{
	
	private static final MQClientInstance instance = new MQClientInstance();
	
	private Map<String,MQProducerInner> producerMap = new HashMap<String, MQProducerInner>();
	
	private String registerCenter;
	private NettyClientConfig nettyClientConfig;
	private RemotingClient remotingClient;
	private IRegisterCenter4Invoker registerCenter4Invoker;
	
	protected volatile List<String> servers;
	
	
	
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "MQProducerInstanceScheduledThread");
        }
    });
	
	@Setter(value=AccessLevel.PRIVATE)
	private volatile boolean started = false;
	@Setter(value=AccessLevel.PRIVATE)
	private volatile boolean shutdown = false;
	
	private MQClientInstance(){
	}
	
	public static MQClientInstance getInstance(){
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
		zkClientRegisterCenter.init();
		initServers();
		startScheduledTask();
		this.started = true;
	}
	
	private void initServers() {
		/**
		 * 获取服务器地址ip:port
		 */
		List<ServerDto> serverDtoList = this.registerCenter4Invoker.getServerList();
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
		this.registerCenter4Invoker.registerServerChanngeListener(this);
	}
	
	private void startScheduledTask() {
		this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                	sendHeartbeatToAllServer();
                } catch (Exception e) {
                    log.error("ScheduledTask sendHeartbeatToAllBroker exception", e);
                }
            }
        }, 100, this.nettyClientConfig.getHeartbeatBrokerInterval(), TimeUnit.MILLISECONDS);
	}
	
	private void sendHeartbeatToAllServer() throws RemotingSendRequestException, RemotingTimeoutException, RemotingConnectException, InterruptedException {
		StringBuffer groups = new StringBuffer();
		for(Entry<String,MQProducerInner> entry : this.producerMap.entrySet()){
			groups.append(entry.getKey()).append(",");
		}
		groups.deleteCharAt(groups.length() - 1);
		for(String address : this.servers){
			RemotingCommand request = RemotingCommand.buildRequestCmd(RequestCode.HEART_BEAT, groups.toString());
			log.debug("Send a heartbea to 【{}】，request：{}",address,request);
			RemotingCommand response = this.remotingClient.invokeSync(address, request, 3000);
			log.debug("Receive heartbeat response：{}， from 【{}】",response,address);
		}
	}
	
	public synchronized void shutdown(){
		if(this.shutdown){
			return;
		}
		this.remotingClient.shutdown();
		this.scheduledExecutorService.shutdown();
		this.registerCenter4Invoker.destroy();
		this.shutdown = true;
	}
	
	@Override
	public void handleServerChannge(List<ServerDto> serverDtoList) {
		List<String> newServers = Lists.newArrayList();
		for (ServerDto dto : serverDtoList) {
			newServers.add(dto.getIp() + ":" + dto.getPort());
		}
		this.servers.clear();
		this.servers.addAll(newServers);
	}
	
	public void registerDefaultRequestProcessor(){
		this.remotingClient.registerDefaultProcessor(
        		new CheckLocalTransactionStateRequestProcessor(), null);
	}
	
	class CheckLocalTransactionStateRequestProcessor implements RequestProcessor{
		@Override
		public RemotingCommand processRequest(ChannelHandlerContext ctx,RemotingCommand request) {
			MQProducerInner inner = producerMap.get(request.getGroup());
			String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
			inner.checkTransactionState(remoteAddress, request.getMessage());
			return null;
		}
	}
}
