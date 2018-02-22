package com.yanghui.elephant.common.message;

import com.yanghui.elephant.common.utils.StringUtil;

/**
 * 
 * @author --小灰灰--
 *
 */
public enum MessageType {
	/**
	 * 普通消息
	 */
	NORMAL_MESSAGE(1),
	/**
	 * 预发送消息
	 */
	TRANSACTION_PRE_MESSAGE(2),
	/**
	 * 提交消息
	 */
	TRANSACTION_COMMIT_MESSAGE(3)
	;
	
	private int type;

	private MessageType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
	
	public static MessageType valueOf(int type) {
		for(MessageType mt : MessageType.values()) {
			if(mt.getType() == type) {
				return mt;
			}
		}
		return null;
	}
	
	public static MessageType valueOfName(String name) {
		if(StringUtil.isEmpty(name)) {
			return MessageType.NORMAL_MESSAGE;
		}
		for(MessageType mt : MessageType.values()) {
			if(mt.name().equals(name)) {
				return mt;
			}
		}
		return MessageType.NORMAL_MESSAGE;
	}
}
