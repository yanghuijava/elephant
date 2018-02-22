package com.yanghui.elephant.common.protocol.header;

import com.yanghui.elephant.common.protocol.CommandCustomHeader;

import lombok.Data;

@Data
public class CheckTransactionStateResponseHeader implements CommandCustomHeader {

	private String messageId;
	
	private String producerGroup;
	/**
	 * 参照LocalTransactionState的值
	 */
	private String commitOrRollback;
}
