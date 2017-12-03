package com.yanghui.elephant.common.constant;

public enum MessageStatus {
	
	CONFIRMING(100,"确认中"),
	ROLLBACK(101,"已回滚"),
	CONFIRMED(102,"已确认"),
	INVALID(103,"已失效"),
	
	;
	
	private int status;
	private String msg;
	private MessageStatus(int status, String msg) {
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
