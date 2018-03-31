package com.yanghui.elephant.example.transaction;

import com.yanghui.elephant.client.producer.LocalTransactionExecuter;
import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.message.Message;

public class LocalTransactionExecuterImpl implements LocalTransactionExecuter {

	@Override
	public LocalTransactionState executeLocalTransactionBranch(Message msg,Object arg) {
		return LocalTransactionState.COMMIT_MESSAGE;
	}
}
