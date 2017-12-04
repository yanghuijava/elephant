package com.yanghui.elephant.remoting.procotol;

import com.yanghui.elephant.remoting.exception.RemotingCommandException;

public interface CommandCustomHeader {
	
	void checkFields() throws RemotingCommandException;

}
