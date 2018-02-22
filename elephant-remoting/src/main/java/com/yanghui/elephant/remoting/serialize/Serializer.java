package com.yanghui.elephant.remoting.serialize;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
/**
 * 自定义序列化抽象类
 * @author --小灰灰--
 *
 */
public abstract class Serializer {

	public static final Charset CHARSET = Charset.forName("UTF-8");
	/**
	 * 写缓冲区
	 */
	protected ByteBuf writeBuffer;
	/**
	 * 读缓冲区
	 */
	protected ByteBuf readBuffer;

	/**
	 * 反序列化具体实现
	 */
	protected abstract void read();

	/**
	 * 序列化具体实现
	 */
	protected abstract void write();

	/**
	 * 从字节数组读取数据
	 * 
	 * @param bytes
	 * @return
	 */
	public Serializer readFromBytes(byte[] bytes) {
		this.readBuffer = Unpooled.wrappedBuffer(bytes);
		read();
		this.readBuffer.clear();
		return this;
	}

	public void readFromBuffer(ByteBuf readBuffer) {
		this.readBuffer = readBuffer;
		read();
	}

	public ByteBuf writeToLocalBuff() {
		writeBuffer = Unpooled.buffer();
		write();
		return writeBuffer;
	}

	public ByteBuf writeToTargetBuff(ByteBuf buffer) {
		writeBuffer = buffer;
		write();
		return writeBuffer;
	}

	public byte[] getBytes() {
		writeToLocalBuff();
		byte[] result = null;
		if (this.writeBuffer.writerIndex() == 0) {
			result = new byte[0];
		} else {
			result = new byte[writeBuffer.writerIndex()];
			this.writeBuffer.readBytes(result);
		}
		return result;
	}

	public byte readByte() {
		return this.readBuffer.readByte();
	}

	public short readShort() {
		return readBuffer.readShort();
	}

	public int readInt() {
		return readBuffer.readInt();
	}

	public long readLong() {
		return readBuffer.readLong();
	}

	public float readFloat() {
		return readBuffer.readFloat();
	}

	public double readDouble() {
		return readBuffer.readDouble();
	}

	public String readString() {
		int size = readBuffer.readShort();
		if (size <= 0) {
			return "";
		}

		byte[] bytes = new byte[size];
		readBuffer.readBytes(bytes);

		return new String(bytes, CHARSET);
	}

	public <T> List<T> readList(Class<T> clz) {
		List<T> list = new ArrayList<>();
		int size = readBuffer.readShort();
		for (int i = 0; i < size; i++) {
			list.add(read(clz));
		}
		return list;
	}

	public <K, V> Map<K, V> readMap(Class<K> keyClz, Class<V> valueClz) {
		Map<K, V> map = new HashMap<>();
		int size = readBuffer.readShort();
		for (int i = 0; i < size; i++) {
			K key = read(keyClz);
			V value = read(valueClz);
			map.put(key, value);
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	private <T> T read(Class<T> clz) {
		Object t = null;
		if (clz == int.class || clz == Integer.class) {
			t = this.readInt();
		} else if (clz == byte.class || clz == Byte.class) {
			t = this.readByte();
		} else if (clz == short.class || clz == Short.class) {
			t = this.readShort();
		} else if (clz == long.class || clz == Long.class) {
			t = this.readLong();
		} else if (clz == float.class || clz == Float.class) {
			t = readFloat();
		} else if (clz == double.class || clz == Double.class) {
			t = readDouble();
		} else if (clz == String.class) {
			t = readString();
		} else if (Serializer.class.isAssignableFrom(clz)) {
			try {
				byte hasObject = this.readBuffer.readByte();
				if (hasObject == 1) {
					Serializer temp = (Serializer) clz.newInstance();
					temp.readFromBuffer(this.readBuffer);
					t = temp;
				} else {
					t = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			throw new RuntimeException(String.format("不支持类型:[%s]", clz));
		}
		return (T) t;
	}

	public Serializer writeByte(Byte value) {
		writeBuffer.writeByte(value);
		return this;
	}

	public Serializer writeShort(Short value) {
		writeBuffer.writeShort(value);
		return this;
	}

	public Serializer writeInt(Integer value) {
		writeBuffer.writeInt(value);
		return this;
	}

	public Serializer writeLong(Long value) {
		writeBuffer.writeLong(value);
		return this;
	}

	public Serializer writeFloat(Float value) {
		writeBuffer.writeFloat(value);
		return this;
	}

	public Serializer writeDouble(Double value) {
		writeBuffer.writeDouble(value);
		return this;
	}

	public <T> Serializer writeList(List<T> list) {
		if (isEmpty(list)) {
			writeBuffer.writeShort((short) 0);
			return this;
		}
		writeBuffer.writeShort((short) list.size());
		for (T item : list) {
			writeObject(item);
		}
		return this;
	}

	public <K, V> Serializer writeMap(Map<K, V> map) {
		if (isEmpty(map)) {
			writeBuffer.writeShort((short) 0);
			return this;
		}
		writeBuffer.writeShort((short) map.size());
		for (Entry<K, V> entry : map.entrySet()) {
			writeObject(entry.getKey());
			writeObject(entry.getValue());
		}
		return this;
	}

	public Serializer writeString(String value) {
		if (value == null || value.isEmpty()) {
			writeShort((short) 0);
			return this;
		}

		byte data[] = value.getBytes(CHARSET);
		short len = (short) data.length;
		writeBuffer.writeShort(len);
		writeBuffer.writeBytes(data);
		return this;
	}

	public Serializer writeObject(Object object) {
		if (object == null) {
			writeByte((byte) 0);
		} else {
			if (object instanceof Integer) {
				writeInt((int) object);
				return this;
			}

			if (object instanceof Long) {
				writeLong((long) object);
				return this;
			}

			if (object instanceof Short) {
				writeShort((short) object);
				return this;
			}

			if (object instanceof Byte) {
				writeByte((byte) object);
				return this;
			}

			if (object instanceof String) {
				String value = (String) object;
				writeString(value);
				return this;
			}
			if (object instanceof Serializer) {
				writeByte((byte) 1);
				Serializer value = (Serializer) object;
				value.writeToTargetBuff(writeBuffer);
				return this;
			}
			throw new RuntimeException("不可序列化的类型:" + object.getClass());
		}
		return this;
	}

	private <T> boolean isEmpty(Collection<T> c) {
		return c == null || c.size() == 0;
	}

	public <K, V> boolean isEmpty(Map<K, V> c) {
		return c == null || c.size() == 0;
	}
}
