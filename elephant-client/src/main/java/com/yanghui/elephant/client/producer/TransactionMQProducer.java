package com.yanghui.elephant.client.producer;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.common.message.Message;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class TransactionMQProducer extends DefaultMQProducer {
	
	private TransactionCheckListener transactionCheckListener;
    private int checkThreadPoolMinSize = 1;
    private int checkThreadPoolMaxSize = 1;
    private int checkRequestHoldMax = 2000;
    
    public TransactionMQProducer(){
    	this(null);
    }
    
    public TransactionMQProducer(String group){
    	super(group);
    }
    
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
		if(null == excuter){
			throw new MQClientException("localTransactionExecuter is null", null);
		}
		return this.defaultMQProducerImpl.sendMessageInTransaction(msg,excuter, arg);
	}
}
