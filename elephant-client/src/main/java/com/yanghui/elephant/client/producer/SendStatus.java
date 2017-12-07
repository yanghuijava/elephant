package com.yanghui.elephant.client.producer;

public enum SendStatus {
	
	SEND_OK,
    FLUSH_DISK_TIMEOUT,
    SEND_MQ_FAIL,
    SERVER_FAIL,
    SEND_FAIL,
    FLUSH_DB_FAIL;
}
