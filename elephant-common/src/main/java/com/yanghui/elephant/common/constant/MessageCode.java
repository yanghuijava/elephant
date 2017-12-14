package com.yanghui.elephant.common.constant;

/**
 * 发送消息的类型
 * @author 杨辉
 *
 */
public enum MessageCode {
	/**
	 * 普通的消息
	 */
	NORMAL_MESSAGE,
	/**
	 * 事务预发送的消息
	 */
	TRANSACTION_PRE_MESSAGE,
	/**
	 * 事务完成的消息
	 */
	TRANSACTION_END_MESSAGE,
	/**
	 * 事务回查的消息
	 */
	TRANSACTION_CHECK_MESSAGE
	;
}
