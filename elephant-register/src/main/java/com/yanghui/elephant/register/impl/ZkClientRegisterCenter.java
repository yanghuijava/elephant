package com.yanghui.elephant.register.impl;

import java.util.List;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.apache.zookeeper.Watcher.Event.KeeperState;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

import com.google.common.collect.Lists;
import com.yanghui.elephant.register.IRegisterCenter4Invoker;
import com.yanghui.elephant.register.IRegisterCenter4Provider;
import com.yanghui.elephant.register.constant.ZKConstant;
import com.yanghui.elephant.register.dto.ProducerDto;
import com.yanghui.elephant.register.dto.ServerDto;
import com.yanghui.elephant.register.listener.IServerChanngeListener;
import com.yanghui.elephant.register.listener.IZKReconnectionListener;

@Log4j2
@Data
public class ZkClientRegisterCenter implements IRegisterCenter4Invoker,IRegisterCenter4Provider {

	private volatile ZkClient zkClient;
	/**
	 * zookeeper地址
	 */
	private String zkAddress = "localhost:2181";
	/**
	 * zkclient会话超时时间
	 */
    private int zkSessionTimeOut = 30000;
    private int zkConnectionTimeOut = 5000;
    
    private IZKReconnectionListener zkReconnectionListener;
    
    private IServerChanngeListener serverChanngeListener;
	
	@Override
	public void init() {
		this.zkClient = new ZkClient(this.zkAddress, this.zkSessionTimeOut, this.zkConnectionTimeOut, new SerializableSerializer());
		initRootPath();
		this.zkClient.subscribeStateChanges(new IZkStateListener() {
			@Override
			public void handleStateChanged(KeeperState state) throws Exception {
				if(zkReconnectionListener != null && state.name().equals(KeeperState.SyncConnected.name())){
					zkReconnectionListener.handleStateForSyncConnected();
				}
			}
			@Override
			public void handleSessionEstablishmentError(Throwable error)throws Exception {
				log.error("处理会话建立错误:{}",error);
			}
			@Override
			public void handleNewSession() throws Exception {
				log.info("会话建立成功！");
			}
		});
	}
	
	private void initRootPath(){
		if(!this.zkClient.exists(ZKConstant.ROOT_PATH)){
			this.zkClient.createPersistent(ZKConstant.ROOT_PATH);
		}
		String serverPath = ZKConstant.ROOT_PATH + ZKConstant.SERVER_PATH;
		if(!this.zkClient.exists(serverPath)){
			this.zkClient.createPersistent(serverPath);
		}
		String producerPath = ZKConstant.ROOT_PATH + ZKConstant.PRODUCER_PATH;
		if(!this.zkClient.exists(producerPath)){
			this.zkClient.createPersistent(producerPath);
		}
	}

	@Override
	public void destroy() {
		this.zkClient.close();
	}

	@Override
	public void addZKReconnectionListener(IZKReconnectionListener zkReconnectionListener) {
		this.zkReconnectionListener = zkReconnectionListener;
	}

	@Override
	public void registerServer(ServerDto serverDto) {
		StringBuffer serverMsgPath = new StringBuffer();
		serverMsgPath.append(serverDto.getServerName())
					 .append(ZKConstant.SEPARATOR)
					 .append(serverDto.getIp())
					 .append(":")
					 .append(serverDto.getPort())
					 .append(ZKConstant.SEPARATOR)
					 .append(serverDto.getDateTime());
		String fullServerMsgPath = ZKConstant.ROOT_PATH + ZKConstant.SERVER_PATH + ZKConstant.ZK_PATH_SEPARATOR
				+ serverMsgPath;
		if(!this.zkClient.exists(fullServerMsgPath)){
			this.zkClient.createEphemeral(fullServerMsgPath);
		}
	}

	@Override
	public void registerProducer(ProducerDto producerDto) {

	}

	@Override
	public List<ServerDto> getServerList() {
		List<ServerDto> result = Lists.newArrayList();
		String serversFullPath = ZKConstant.ROOT_PATH + ZKConstant.SERVER_PATH;
		List<String> servers = this.zkClient.getChildren(serversFullPath);
		if(servers == null || servers.isEmpty()){
			return result;
		}
		for(String server : servers){
			ServerDto dto = analysisServerPath(server);
			result.add(dto);
		}
		if(this.serverChanngeListener != null){
			this.zkClient.subscribeChildChanges(serversFullPath, new IZkChildListener() {
				@Override
				public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
					log.info("server is channge parantPath：{}，children：{}",parentPath,currentChilds);
					List<ServerDto> result = Lists.newArrayList();
					for(String serverPath : currentChilds){
						result.add(analysisServerPath(serverPath));
					}
					serverChanngeListener.handleServerChannge(result);
				}
			});
		}
		return result;
	}
	
	private ServerDto analysisServerPath(String serverPath){
		String[] arr = serverPath.split(ZKConstant.SEPARATOR);
		ServerDto dto = new ServerDto();
		dto.setServerName(arr[0]);
		dto.setDateTime(arr[2]);
		String[] ipPort = arr[1].split(":");
		dto.setIp(ipPort[0]);
		dto.setPort(Integer.valueOf(ipPort[1]));
		return dto;
	}

	@Override
	public List<ProducerDto> getProducerByGroup(String group) {
		return null;
	}

	@Override
	public void registerServerChanngeListener(IServerChanngeListener serverChanngeListener) {
		this.serverChanngeListener = serverChanngeListener;
	}
}
