package com.yanghui.elephant.remoting.serialize;
/**
 * 
 * @author --小灰灰--
 *
 */
public enum SerializeType {
	/**
	 * json序列化
	 */
    JSON((byte) 0),
    /**
     * 自定义序列化
     */
    CUSTOM((byte)1),
    /**
     * hessian序列化
     */
    HESSIAN((byte)2),
    ;

    private byte code;

    SerializeType(byte code) {
        this.code = code;
    }

    public static SerializeType valueOf(byte code) {
        for (SerializeType serializeType : SerializeType.values()) {
            if (serializeType.getCode() == code) {
                return serializeType;
            }
        }
        return null;
    }

    public byte getCode() {
        return code;
    }
}
