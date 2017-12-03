package com.yanghui.elephant.remoting.procotol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;

@Data
public class RemotingCommand implements Serializable{
	
	private static final long serialVersionUID = -72755327970399018L;

	private static AtomicInteger requestId = new AtomicInteger(0);
	
	private int code;
	private int unique = requestId.getAndIncrement();
	private Object body;
	private String remark;
	private HashMap<String, Object> extFields;
	
	private RemotingCommandType type;
}
