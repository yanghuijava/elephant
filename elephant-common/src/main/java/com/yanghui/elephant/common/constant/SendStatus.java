package com.yanghui.elephant.common.constant;

public enum SendStatus {
	
	WAIT_SEND(1,"待发送"),
	ALREADY_SEND(2,"已发送"),
	;
	
	private int status;
	private String msg;
	private SendStatus(int status, String msg) {
		this.status = status;
		this.msg = msg;
	}
	public int getStatus() {
		return status;
	}
	public String getMsg() {
		return msg;
	}

}
