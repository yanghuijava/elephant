package com.yanghui.elephant.register.dto;

import lombok.Data;

@Data
public class ProducerDto {
	
	private String group;
	private String ip;
	private int checkThreadPoolMinSize;//Broker回查Producer事务状态时，线程池大小
	private int checkThreadPoolMaxSize;//Broker回查Producer事务状态时，线程池大小
	private int checkRequestHoldMax;//Broker回查Producer事务状态时，Produceer本地缓冲请求队列大小

}
