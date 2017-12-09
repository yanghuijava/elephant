package com.yanghui.elephant.client.producer;

import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.message.Message;

public class TransactionCheckListenerImpl implements TransactionCheckListener {

	@Override
	public LocalTransactionState checkLocalTransactionState(Message msg) {
		return LocalTransactionState.COMMIT_MESSAGE;
	}
}
