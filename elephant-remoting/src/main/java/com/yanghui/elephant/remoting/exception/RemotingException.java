package com.yanghui.elephant.remoting.exception;

public class RemotingException extends Exception {

	private static final long serialVersionUID = -7689095117620948056L;

	public RemotingException(String message) {
		super(message);
	}

	public RemotingException(String message, Throwable cause) {
		super(message, cause);
	}
}
