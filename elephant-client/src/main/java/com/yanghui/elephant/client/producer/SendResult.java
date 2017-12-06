package com.yanghui.elephant.client.producer;

import lombok.Data;

@Data
public class SendResult {
	
	private SendStatus sendStatus;
    private String msgId;

}
