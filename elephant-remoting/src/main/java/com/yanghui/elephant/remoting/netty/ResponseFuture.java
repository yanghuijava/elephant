package com.yanghui.elephant.remoting.netty;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.Data;

import com.yanghui.elephant.remoting.procotol.RemotingCommand;

@Data
public class ResponseFuture {
	
	private final CountDownLatch countDownLatch = new CountDownLatch(1);
	
	private final int unique;
	
	//结果返回时间
    private long responseTime;
    
    //超时时间
    private final long timeoutMillis;
    
    //结果
    private volatile RemotingCommand remotingCommand;
    
    private volatile boolean sendRequestOK = true;
    
    //开始时间
    private final long beginTimestamp = System.currentTimeMillis();
    
    private volatile Throwable cause;
    
    public RemotingCommand waitRespose(long timeout) throws InterruptedException {
		this.countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
		return this.remotingCommand;
	}

	public void putResponse(RemotingCommand response) {
		this.remotingCommand = response;
		this.responseTime = System.currentTimeMillis();
		this.countDownLatch.countDown();
	}

	public boolean isTimeout() {
		long diff = System.currentTimeMillis() - this.beginTimestamp;
        return diff > this.timeoutMillis;
	}

	public ResponseFuture(int unique, long timeoutMillis) {
		this.unique = unique;
		this.timeoutMillis = timeoutMillis;
	}
}
