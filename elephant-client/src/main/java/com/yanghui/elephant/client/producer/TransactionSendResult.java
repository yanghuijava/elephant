package com.yanghui.elephant.client.producer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.yanghui.elephant.common.constant.LocalTransactionState;

@Data
@EqualsAndHashCode(callSuper=false)
@ToString(callSuper=true)
public class TransactionSendResult extends SendResult {
	
	private LocalTransactionState localTransactionState;

}
