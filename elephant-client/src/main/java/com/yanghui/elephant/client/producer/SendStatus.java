package com.yanghui.elephant.client.producer;

public enum SendStatus {
	/**
	 * 发送成功
	 */
	SEND_OK,
	/**
	 * 发送超时
	 */
    FLUSH_DISK_TIMEOUT,
    /**
     * 发送mq失败
     */
    SEND_MQ_FAIL,
    /**
     * 保存失败
     */
    SERVER_FAIL,
    /**
     * 发送失败
     */
    SEND_FAIL,
    /**
     * 保存数据库失败
     */
    FLUSH_DB_FAIL;
}
