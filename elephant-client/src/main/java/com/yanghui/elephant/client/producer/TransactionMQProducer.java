package com.yanghui.elephant.client.producer;

import com.yanghui.elephant.client.exception.MQClientException;
import com.yanghui.elephant.common.message.Message;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class TransactionMQProducer extends DefaultMQProducer {
	/**
	 * 消息回查监听器
	 */
	private TransactionCheckListener transactionCheckListener;
	/**
	 * 回查的线程池的最小值
	 */
    private int checkThreadPoolMinSize = 1;
    /**
     * 回查的线程池的最大值
     */
    private int checkThreadPoolMaxSize = 1;
    /**
     * 回查的最大请求数
     */
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
		return this.defaultMQProducerImpl.sendMessageInTransaction(msg,excuter,arg);
	}
}
