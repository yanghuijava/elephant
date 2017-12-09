package com.yanghui.elephant.common.constant;

public enum LocalTransactionState {
	/**
	 * 预发送的消息
	 */
	PRE_MESSAGE,
	/**
	 * 提交消息
	 */
	COMMIT_MESSAGE,
	/**
	 * 回滚消息
	 */
    ROLLBACK_MESSAGE,
    /**
     * 未知消息
     */
    UNKNOW,
}
