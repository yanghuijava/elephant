package com.yanghui.elephant.mq.producer;

import com.yanghui.elephant.common.message.Message;

public interface ProducerService {
	
	void sendMessage(final Message message);

}
