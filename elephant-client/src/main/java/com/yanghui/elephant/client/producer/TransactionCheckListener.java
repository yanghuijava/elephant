package com.yanghui.elephant.client.producer;

import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.message.Message;

public interface TransactionCheckListener {
	
	LocalTransactionState checkLocalTransactionState(final Message msg);

}
