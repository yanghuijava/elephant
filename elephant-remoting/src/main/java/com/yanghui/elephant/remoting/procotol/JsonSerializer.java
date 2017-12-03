package com.yanghui.elephant.remoting.procotol;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * <B>中文类名：fastjson实现序列化</B><BR>
 * <B>概要说明：</B><BR>
 * @author 贸易研发部：yanghui（think）
 * @since 2017年8月6日
 */
public class JsonSerializer implements ISerializer {

	@Override
	public <T> byte[] serializer(T obj) {
		JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
		return JSON.toJSONBytes(obj, SerializerFeature.WriteDateUseDateFormat);
	}

	@Override
	public <T> T deserializer(byte[] data, Class<T> clazz) {
		return JSON.parseObject(new String(data), clazz);
	}
}
