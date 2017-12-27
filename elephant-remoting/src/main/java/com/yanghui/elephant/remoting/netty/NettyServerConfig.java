package com.yanghui.elephant.remoting.netty;

import lombok.Data;

@Data
public class NettyServerConfig implements Cloneable {
    private int listenPort = 8888;
    private int serverWorkerThreads = 8;
    private int serverCallbackExecutorThreads = 0;
    private int serverSelectorThreads = 3;
    private int serverOnewaySemaphoreValue = 256;
    private int serverAsyncSemaphoreValue = 64;
    private int serverChannelMaxIdleTimeSeconds = 120;

    private int serverSocketSndBufSize = 65535;
    private int serverSocketRcvBufSize = 65535;
    private boolean serverPooledByteBufAllocatorEnable = true;

    private boolean useEpollNativeSelector = false;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (NettyServerConfig) super.clone();
    }
}
