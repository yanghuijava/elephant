package com.yanghui.elephant.common.protocol.header;

import com.yanghui.elephant.common.protocol.CommandCustomHeader;

import lombok.Data;

@Data
public class SendMessageRequestHeader implements CommandCustomHeader{
	
	private String producerGroup;
	
	private String messageId;
	
	private String destination;
	
	private int MessageType;
	
	private String properties;

}
