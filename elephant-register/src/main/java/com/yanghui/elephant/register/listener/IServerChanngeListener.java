package com.yanghui.elephant.register.listener;

import java.util.List;

import com.yanghui.elephant.register.dto.ServerDto;

public interface IServerChanngeListener {
	
	void handleServerChannge(List<ServerDto> serverDtoList);

}
