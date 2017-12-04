package com.yanghui.elephant.register.dto;

import lombok.Data;

@Data
public class ServerDto {
	
	private String serverName;
	private String ip;
	private int port;
	private String dateTime;

}
