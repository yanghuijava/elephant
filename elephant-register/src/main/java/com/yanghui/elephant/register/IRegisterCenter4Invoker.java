package com.yanghui.elephant.register;

import java.util.List;

import com.yanghui.elephant.register.dto.ProducerDto;
import com.yanghui.elephant.register.dto.ServerDto;
import com.yanghui.elephant.register.listener.IServerChanngeListener;

public interface IRegisterCenter4Invoker extends IRegisterCenter{
	
	List<ServerDto> getServerList();
	
	List<ProducerDto> getProducerByGroup(String group);
	
	void registerServerChanngeListener(IServerChanngeListener serverChanngeListener);

}
