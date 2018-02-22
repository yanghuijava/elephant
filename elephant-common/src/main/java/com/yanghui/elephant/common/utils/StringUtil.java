package com.yanghui.elephant.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

	public static boolean isNumeric(String str) {
		Pattern pattern = Pattern.compile("[0-9]*");
		Matcher isNum = pattern.matcher(str);
		if (!isNum.matches()) {
			return false;
		}
		return true;
	}
	
	public static boolean isEmpty(String src) {
		if(src == null || "".equals(src)) {
			return true;
		}
		return false;
	}
}
