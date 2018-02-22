package com.yanghui.elephant.common.constant;

public interface RequestCode {
	/**
	 * 发送消息
	 */
	public static final int SEND_MESSAGE = 10;
	/**
	 * 心跳
	 */
	public static final int HEART_BEAT = 11;
	/**
	 * 结束事务消息
	 */
	public static final int END_TRANSACTION = 12;
	/**
	 * 事务回查消息
	 */
	public static final int CHECK_TRANSACTION = 13;
	/**
	 * 事务回查结果消息
	 */
	public static final int CHECK_TRANSACTION_RESPONSE = 14;
}
