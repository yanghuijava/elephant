package com.yanghui.elephant.remoting.serialize;

public interface ISerializer {
	
	/**
	 * <B>方法名称：序列化</B><BR>
	 * <B>概要说明：</B><BR>
	 * @param obj
	 * @return
	 */
	public <T> byte[] serializer(T obj);
	/**
	 * <B>方法名称：反序列化</B><BR>
	 * <B>概要说明：</B><BR>
	 * @param data
	 * @param clazz
	 * @return
	 */
	public <T> T deserializer(byte[] data,Class<T> clazz);

}
