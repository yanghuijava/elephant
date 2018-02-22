package com.yanghui.elephant.common.protocol.header;

import com.yanghui.elephant.common.protocol.CommandCustomHeader;

import lombok.Data;

@Data
public class EndTransactionRequestHeader implements CommandCustomHeader {
	
	private String producerGroup;
	
	private String msgId;
	/**
	 * 参照LocalTransactionState的值
	 */
	private String commitOrRollback;
}
