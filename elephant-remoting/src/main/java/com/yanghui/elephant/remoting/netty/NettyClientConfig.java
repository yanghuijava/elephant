package com.yanghui.elephant.remoting.netty;

import lombok.Data;
/**
 * 
 * @author --小灰灰--
 *
 */
@Data
public class NettyClientConfig {
    private int clientWorkerThreads = 4;
    private int clientCallbackExecutorThreads = Runtime.getRuntime().availableProcessors();
    private int clientOnewaySemaphoreValue = 65535;
    private int clientAsyncSemaphoreValue = 65535;
    private int connectTimeoutMillis = 3000;
    private long channelNotActiveInterval = 1000 * 60;

    private int clientChannelMaxIdleTimeSeconds = 120;

    private int clientSocketSndBufSize = 65535;
    private int clientSocketRcvBufSize = 65535;
    private boolean clientPooledByteBufAllocatorEnable = false;
    private boolean clientCloseSocketIfTimeout = false;
    
    private int heartbeatBrokerInterval = 1000 * 30;

}
