package com.yanghui.elephant.common.constant;

public enum RemotingCommandCode {
	/**
	 * 普通消息
	 */
	NORMAL_MESSAGE,
	/**
	 * 事务预发送消息
	 */
	TRANSACTION_PRE_MESSAGE,
	/**
	 * 事务完成消息
	 */
	TRANSACTION_END_MESSAGE,
	/**
	 * 事务回查消息
	 */
	TRANSACTION_CHECK_MESSAGE
	;
}
