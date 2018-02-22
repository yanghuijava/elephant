package com.yanghui.elephant.remoting.exception;
/**
 * 
 * @author --小灰灰--
 *
 */
public class RemotingSendRequestException extends RemotingException {
	
	private static final long serialVersionUID = -8152083905058314464L;

	public RemotingSendRequestException(String addr) {
        this(addr, null);
    }

    public RemotingSendRequestException(String addr, Throwable cause) {
        super("send request to <" + addr + "> failed", cause);
    }

}
