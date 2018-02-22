package com.yanghui.elephant.client.producer;

import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.message.Message;

/**
 * 本地事务执行器接口
 * @author --小灰灰--
 *
 */
public interface LocalTransactionExecuter {
	
	public LocalTransactionState executeLocalTransactionBranch(final Message msg, final Object arg);

}
