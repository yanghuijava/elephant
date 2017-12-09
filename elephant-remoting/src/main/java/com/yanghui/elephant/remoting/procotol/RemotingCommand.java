package com.yanghui.elephant.remoting.procotol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.yanghui.elephant.common.constant.RemotingCommandCode;

import lombok.Data;

@Data
public class RemotingCommand implements Serializable{
	
	private static final long serialVersionUID = -72755327970399018L;

	private static AtomicInteger requestId = new AtomicInteger(0);
	
	private int code;
	private int unique;
	private Object body;
	private String remark;
	private HashMap<String, Object> extFields;
	private CommandCustomHeader customHeader;
	private RemotingCommandCode remotingCommandCode;
	
	private RemotingCommandType type;
	
	public static RemotingCommand buildRequestCmd(Object body,CommandCustomHeader customHeader,RemotingCommandCode remotingCommandCode){
		RemotingCommand cmd = new RemotingCommand();
		cmd.setUnique(requestId.getAndIncrement());
		cmd.setBody(body);
		cmd.setCustomHeader(customHeader);
		cmd.setType(RemotingCommandType.REQUEST_COMMAND);
		cmd.setRemotingCommandCode(remotingCommandCode);
		return cmd;
	}
	
	public static RemotingCommand buildResposeCmd(int code,int unique){
		RemotingCommand cmd = new RemotingCommand();
		cmd.setCode(code);
		cmd.setType(RemotingCommandType.RESPONSE_COMMAND);
		cmd.setUnique(unique);
		return cmd;
	}
}
