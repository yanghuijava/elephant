package com.yanghui.elephant.remoting.procotol.header;

import java.io.Serializable;

import lombok.Data;

import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.remoting.exception.RemotingCommandException;
import com.yanghui.elephant.remoting.procotol.CommandCustomHeader;
@Data
public class MessageRequestHeader implements CommandCustomHeader,Serializable {
	
	private static final long serialVersionUID = 7337405667020443161L;
	
	private String group;
	private LocalTransactionState localTransactionState;

	@Override
	public void checkFields() throws RemotingCommandException {

	}
}
