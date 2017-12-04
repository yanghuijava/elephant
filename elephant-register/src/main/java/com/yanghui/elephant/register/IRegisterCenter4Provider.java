package com.yanghui.elephant.register;

import com.yanghui.elephant.register.dto.ProducerDto;
import com.yanghui.elephant.register.dto.ServerDto;

public interface IRegisterCenter4Provider extends IRegisterCenter {
	
	void registerServer(ServerDto serverDto);

	void registerProducer(ProducerDto producerDto);
}
