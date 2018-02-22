package com.yanghui.elephant.remoting.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import lombok.extern.log4j.Log4j2;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
/**
 * 
 * @author --小灰灰--
 *
 */
@Log4j2
public class HessianSerializer implements ISerializer {
	
	@Override
	public <T> byte[] serializer(T obj) {
		if(obj == null){
			throw new NullPointerException();
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		HessianOutput ho = null;
		try {
			ho = new HessianOutput(out);
			ho.writeObject(obj);
			return out.toByteArray();
		} catch (Exception e) {
			log.error("hessian序列化发生异常：{}", e);
			throw new RuntimeException(e);
		}finally{
			try {
				if(ho != null)ho.close();
			} catch (IOException e) {
				log.error("hessian序列化发生异常：{}", e);
				throw new RuntimeException(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserializer(byte[] data, Class<T> clazz) {
		if(data == null){
			throw new NullPointerException();
		}
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		HessianInput hi = null;
		try {
			hi = new HessianInput(in);
			return (T)hi.readObject();
		} catch (IOException e) {
			log.error("hessian反序列化发生异常：{}", e);
			throw new RuntimeException(e);
		}finally{
			hi.close();
		}
	}
}
