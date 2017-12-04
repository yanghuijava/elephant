package com.yanghui.elephant.remoting.exception;

public class RemotingCommandException extends RemotingException {
	
	private static final long serialVersionUID = 4077199346681215081L;

	public RemotingCommandException(String message) {
        super(message, null);
    }

    public RemotingCommandException(String message, Throwable cause) {
        super(message, cause);
    }

}
