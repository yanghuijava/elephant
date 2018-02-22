package com.yanghui.elephant.remoting.serialize;

import lombok.extern.log4j.Log4j2;
/**
 * 
 * @author --小灰灰--
 *
 */
@Log4j2
public class CostomSerializer implements ISerializer {

	@Override
	public <T> byte[] serializer(T obj) {
		Serializer s = (Serializer)obj;
		return s.getBytes();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserializer(byte[] data, Class<T> clazz) {
		try {
			Serializer result = (Serializer)clazz.newInstance();
			result.readFromBytes(data);
			return (T)result;
		} catch (Exception e) {
			log.error("序列化发生异常：{}",e);
			throw new RuntimeException(e);
		}
	}
}