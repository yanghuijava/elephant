package com.yanghui.elephant.client.impl;

import com.yanghui.elephant.common.message.Message;
/**
 * 
 * @author yanghui
 *
 */
public interface MQProducerInner {
	/**
	 * 查询事务状态
	 * @param address
	 * @param msg
	 */
	void checkTransactionState(final String address,final Message msg);
	
}
