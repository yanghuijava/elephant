package com.yanghui.elephant.common.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Data;

/**
 * 
 * @author yanghui
 *
 */
@Data
public class Message implements Serializable{
	
	private static final long serialVersionUID = -1801619424388464167L;
	private String messageId;
	private String destination;
	private byte[] body;
	private Map<String,String> properties = new HashMap<>();
	
	public Message(){
	}
	
	public Message(String messageId){
		this.messageId= messageId;
	}
	
	public Message(String destination,byte[] body){
		this.destination = destination;
		this.body = body;
		this.messageId = UUID.randomUUID().toString();
	}
}
