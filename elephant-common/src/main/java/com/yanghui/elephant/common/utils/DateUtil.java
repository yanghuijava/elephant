package com.yanghui.elephant.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
	
	private DateUtil(){
		throw new IllegalStateException("Utility class");
	}
	
	public static String formatAsDateTime(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}
	
	public static String formatAsDate(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(date);
	}
	
	public static String formatAsDateMilli(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return sdf.format(date);
	}
}
