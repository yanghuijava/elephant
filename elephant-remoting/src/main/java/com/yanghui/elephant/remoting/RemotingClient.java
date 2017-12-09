package com.yanghui.elephant.remoting;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.yanghui.elephant.remoting.exception.RemotingConnectException;
import com.yanghui.elephant.remoting.exception.RemotingSendRequestException;
import com.yanghui.elephant.remoting.exception.RemotingTimeoutException;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;

public interface RemotingClient extends RemotingService{
	
	public void updateRegisterCenterAddressList(final List<String> addrs);

    public List<String> getRegisterCenterAddressList();
    /**
     * 同步远程调用
     * @param addr 远程地址
     * @param request 请求参数
     * @param timeoutMillis 超时时间
     * @return
     */
    public RemotingCommand invokeSync(final String addr, final RemotingCommand request,final long timeoutMillis)
    		throws InterruptedException,RemotingSendRequestException,RemotingTimeoutException,RemotingConnectException;
    
    void invokeOneway(final String addr, final RemotingCommand request,final long timeoutMillis)throws InterruptedException, RemotingSendRequestException,RemotingTimeoutException,RemotingConnectException;

    void registerDefaultProcessor(final RequestProcessor processor, final ExecutorService executor);
}
