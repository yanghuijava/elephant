package com.yanghui.elephant.common.constant;

public enum LocalTransactionState {
	/**
	 * 提交消息
	 */
	COMMIT_MESSAGE,
	/**
	 * 回滚消息
	 */
    ROLLBACK_MESSAGE,
    /**
     * 未知消息
     */
    UNKNOW,
    ;
	
	public static LocalTransactionState valueOfName(String name) {
		for(LocalTransactionState state : LocalTransactionState.values()) {
			if(state.name().equals(name)) {
				return state;
			}
		}
		return null;
	}
}
