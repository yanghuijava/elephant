package com.yanghui.elephant.remoting.netty;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import com.yanghui.elephant.remoting.procotol.RemotingCommandType;
import com.yanghui.elephant.remoting.procotol.header.MessageRequestHeader;

public class NettyRemotingClientTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() throws Exception {
		NettyRemotingClient client = new NettyRemotingClient(new NettyClientConfig());
		client.start();
		RemotingCommand request = new RemotingCommand();
		MessageRequestHeader messageRequestHeader = new MessageRequestHeader();
		messageRequestHeader.setGroup("test3");
		messageRequestHeader.setLocalTransactionState(LocalTransactionState.PRE_MESSAGE);
		request.setType(RemotingCommandType.REQUEST_COMMAND);
		request.setBody(new Message("queue://test2","我是消息".getBytes()));
		request.setCustomHeader(messageRequestHeader);
		
		RemotingCommand response = client.invokeSync("127.0.0.1:8888", request, 3000L);
		System.out.println("响应的消息：" + response);
		client.shutdown();
	}
}
