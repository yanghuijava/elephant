package com.yanghui.elephant.remoting;

import java.util.concurrent.ExecutorService;

import com.yanghui.elephant.remoting.procotol.RemotingCommand;

public interface RemotingServer extends RemotingService{
	
	int localListenPort();
	
	void registerDefaultProcessor(final RequestProcessor processor, final ExecutorService executor);
	
	void sendToClient(RemotingCommand request);

}
