package com.yanghui.elephant.client.producer;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.remoting.procotol.header.MessageRequestHeader;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class TransactionMQProducer extends DefaultMQProducer {
	
	private TransactionCheckListener transactionCheckListener;
    private int checkThreadPoolMinSize = 1;
    private int checkThreadPoolMaxSize = 1;
    private int checkRequestHoldMax = 2000;
	@Override
	public void start() throws MQClientException {
		super.start();
		this.defaultMQProducerImpl.initTransaction();
	}
	@Override
	public void shutdown() {
		super.shutdown();
		this.defaultMQProducerImpl.destroyTransaction();
	}
	
	@Override
	public TransactionSendResult sendMessageTransaction(Message msg,LocalTransactionExecuter excuter, Object arg) throws MQClientException{
		if (null == this.transactionCheckListener) {
            throw new MQClientException("localTransactionBranchCheckListener is null", null);
        }
		MessageRequestHeader header = new MessageRequestHeader();
		header.setGroup(this.producerGroup);
		header.setLocalTransactionState(LocalTransactionState.PRE_MESSAGE);
		header.setTransaction(true);
		return this.defaultMQProducerImpl.sendMessageInTransaction(msg, header, excuter, arg);
	}
}
