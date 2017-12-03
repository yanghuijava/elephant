package com.yanghui.elephant.remoting.exception;

public class RemotingConnectException extends RemotingException {

	private static final long serialVersionUID = -1761263285779090897L;

	public RemotingConnectException(String addr) {
		this(addr, null);
	}

	public RemotingConnectException(String addr, Throwable cause) {
		super("connect to <" + addr + "> failed", cause);
	}
}
