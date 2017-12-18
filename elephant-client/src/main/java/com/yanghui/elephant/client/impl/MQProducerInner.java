package com.yanghui.elephant.client.impl;

import com.yanghui.elephant.common.message.Message;

public interface MQProducerInner {
	
	void checkTransactionState(final String address,final Message msg);
	
}
