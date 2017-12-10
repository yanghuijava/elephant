package com.yanghui.elephant.example.transaction;

import com.yanghui.elephant.client.producer.TransactionCheckListener;
import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.message.Message;

public class TransactionCheckListenerImpl implements TransactionCheckListener {

	@Override
	public LocalTransactionState checkLocalTransactionState(Message msg) {
		System.err.println("回查：" + msg);
		return LocalTransactionState.COMMIT_MESSAGE;
	}
}
