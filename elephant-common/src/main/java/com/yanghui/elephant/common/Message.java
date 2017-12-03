package com.yanghui.elephant.common;

import java.util.Map;

import lombok.Data;

/**
 * 
 * @author yanghui
 *
 */
@Data
public class Message {
	
	private String destination;
	private byte[] body;
	private Map<String,Object> properties;

}
