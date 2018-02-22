package com.yanghui.elephant.remoting;

import java.util.concurrent.ExecutorService;
/**
 * 
 * @author --小灰灰--
 *
 */
public interface RemotingServer extends RemotingService{
	
	int localListenPort();
	
	void registerDefaultProcessor(final RequestProcessor processor, final ExecutorService executor);
	
	void registerProcessor(int requestCode,final RequestProcessor processor, final ExecutorService executor);

}
