package com.yanghui.elephant.remoting.procotol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.constant.MessageCode;
import com.yanghui.elephant.common.message.Message;

import lombok.Data;

@Data
public class RemotingCommand implements Serializable{
	
	private static final long serialVersionUID = -72755327970399018L;

	private static AtomicInteger requestId = new AtomicInteger(0);
	
	private int code;
	private int unique;
	private String remark;
	private HashMap<String, String> extFields;
	private MessageCode messageCode;
	private RemotingCommandType type;
	private Message message;
	private LocalTransactionState localTransactionState;
	private String group;
	
	public static RemotingCommand buildRequestCmd(int requestCode,String remark){
		RemotingCommand cmd = new RemotingCommand();
		cmd.setUnique(requestId.getAndIncrement());
		cmd.setType(RemotingCommandType.REQUEST_COMMAND);
		cmd.setCode(requestCode);
		cmd.setRemark(remark);
		return cmd;
	}
	
	public static RemotingCommand buildRequestCmd(Message message,int requestCode,MessageCode messageCode){
		RemotingCommand cmd = new RemotingCommand();
		cmd.setUnique(requestId.getAndIncrement());
		cmd.setMessage(message);
		cmd.setType(RemotingCommandType.REQUEST_COMMAND);
		cmd.setMessageCode(messageCode);
		cmd.setCode(requestCode);
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
