package com.yanghui.elephant.remoting.procotol;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.yanghui.elephant.common.protocol.CommandCustomHeader;
import com.yanghui.elephant.remoting.serialize.SerializeType;
import com.yanghui.elephant.remoting.serialize.Serializer;
import com.yanghui.elephant.remoting.serialize.SerializerEngine;

import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 远程通信数据包类
 * @author --小灰灰--
 *
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class RemotingCommand extends Serializer implements Serializable{
	
	private static final long serialVersionUID = -72755327970399018L;
	/**
	 * 请求命令
	 */
	private static final int REQUEST_CMD_TYPE = 0;
	/**
	 * 响应命令
	 */
	private static final int RESPONSE_CMD_TYPE = 1;

	private static AtomicInteger requestId = new AtomicInteger(0);
	private static Map<Class<? extends CommandCustomHeader>, Field[]> CLASS_FIELD_MAP = new HashMap<>();
	
	private int code;
	private int version = 0;
	private int unique;
	private int flag;
	private String remark;
	private Map<String, String> extFields;
	private transient CommandCustomHeader commandCustomHeader;
	private transient byte[] body;
	private SerializeType serializeType = SerializeType.CUSTOM;
	
	
	public static RemotingCommand buildRequestCmd(CommandCustomHeader commandCustomHeader,int requestCode){
		RemotingCommand cmd = new RemotingCommand();
		cmd.setUnique(requestId.getAndIncrement());
		cmd.setCode(requestCode);
		cmd.setFlag(REQUEST_CMD_TYPE);
		cmd.setCommandCustomHeader(commandCustomHeader);
		return cmd;
	}
	
	public static RemotingCommand buildRequestCmd(int requestCode,String remark){
		RemotingCommand cmd = new RemotingCommand();
		cmd.setUnique(requestId.getAndIncrement());
		cmd.setFlag(REQUEST_CMD_TYPE);
		cmd.setCode(requestCode);
		cmd.setRemark(remark);
		return cmd;
	}
	
	public static RemotingCommand buildResposeCmd(int code,int unique){
		RemotingCommand cmd = new RemotingCommand();
		cmd.setCode(code);
		cmd.setFlag(RESPONSE_CMD_TYPE);
		cmd.setUnique(unique);
		return cmd;
	}
	
	/**
	 * 获取远程命令类型
	 * @return
	 */
	public RemotingCommandType getRemotingCommandType() {
		if(this.flag == REQUEST_CMD_TYPE) {
			return RemotingCommandType.REQUEST_COMMAND;
		}
		return RemotingCommandType.RESPONSE_COMMAND;
	}
	
	private Field[] getClazzFields(Class<? extends CommandCustomHeader> clz) {
		Field[] result = CLASS_FIELD_MAP.get(clz);
		if(result != null) {
			return result;
		}
		synchronized (CLASS_FIELD_MAP) {
			if(CLASS_FIELD_MAP.get(clz) != null) {
				return CLASS_FIELD_MAP.get(clz);
			}
			result = clz.getDeclaredFields();
			CLASS_FIELD_MAP.put(clz, result);
		}
		return result;
	}
	
	public CommandCustomHeader decodeCommandCustomHeader(Class<? extends CommandCustomHeader> clazz) {
		CommandCustomHeader commandCustomHeader = null;
		try {
			commandCustomHeader = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
		if(this.extFields == null) {
			return commandCustomHeader;
		}
		Field[] fields = getClazzFields(commandCustomHeader.getClass());
		for(Field f : fields) {
			if(Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			f.setAccessible(true);
			try {
				String fieldName = f.getName();
				String value = this.extFields.get(fieldName);
				Class<?> clz = f.getType();
				if(clz == int.class || clz == Integer.class) {
					f.set(commandCustomHeader, Integer.valueOf(value));
				}else if(clz == short.class || clz == Short.class) {
					f.set(commandCustomHeader, Short.valueOf(value));
				}else if(clz == float.class || clz == Float.class) {
					f.set(commandCustomHeader, Float.valueOf(value));
				}else if(clz == double.class || clz == Double.class) {
					f.set(commandCustomHeader, Double.valueOf(value));
				}else if(clz == byte.class || clz == Byte.class) {
					f.set(commandCustomHeader, Byte.valueOf(value));
				}else if(clz == long.class || clz == Long.class) {
					f.set(commandCustomHeader, Long.valueOf(value));
				}else if(clz == char.class || clz == Character.class) {
					f.set(commandCustomHeader, value.charAt(0));
				}else if(clz == boolean.class || clz == Boolean.class) {
					f.set(commandCustomHeader, Boolean.valueOf(value));
				}else if(clz == String.class) {
					f.set(commandCustomHeader, value);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			f.setAccessible(false);
		}
		this.commandCustomHeader = commandCustomHeader;
		return commandCustomHeader;
	}
	
	private void makeCommandCustomHeaderToExtFields() {
		if(null == this.commandCustomHeader) {
			return;
		}
		if(null == this.extFields) {
			this.extFields = new HashMap<>();
		}
		Field[] fields = getClazzFields(commandCustomHeader.getClass());
		for(Field f : fields) {
			if(Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			String fieldName = f.getName();
			try {
				f.setAccessible(true);
				Object value = f.get(this.commandCustomHeader);
				f.setAccessible(false);
				this.extFields.put(fieldName, String.valueOf(value));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * <p>编码报文</p>
	 * <p>
	 * 报文格式：</br>
	 * +----------+-----------------+---------------+--------------+</br>
	 * |  总长度         | header length   |  header data  |  body data   |</br>
	 * +----------|-----------------|---------------+--------------+</br>
	 * </p>
	 * @return
	 */
	public byte[] encode() {
		/**
		 * 报文的总长度
		 */
		int length = 4;
		int bodyDataLength = this.body == null ? 0 : this.body.length;
		byte[] headerData = headerEncode();
		length += headerData.length;
		length += bodyDataLength;
		
		ByteBuffer result = ByteBuffer.allocate(4 + 4 + headerData.length + bodyDataLength);
		result.putInt(length);
		result.put(signSerializeType(headerData.length, this.serializeType));
		result.put(headerData);
		if(this.body != null) {
			result.put(this.body);
		}
		return result.array();
	}
	
	public static RemotingCommand decode(ByteBuffer buffer) {
		int length = buffer.limit();
		int headerLength = buffer.getInt();
		/**
		 * 获取序列化类型
		 */
		SerializeType type = SerializeType.valueOf((byte)((headerLength >> 3*8) & 0xFF));
		/**
		 * 获取header的真实长度
		 */
		int realHeaderLength = headerLength & 0x0FFF;
		byte[] headerData = new byte[realHeaderLength];
		buffer.get(headerData);
		RemotingCommand result = headerDecode(headerData,type);
		int bodyLength = length - 4 - realHeaderLength;
		byte[] bodyData = new byte[bodyLength];
		buffer.get(bodyData);
		
		result.setBody(bodyData);
		result.setSerializeType(type);
		return result;
	}
	
	private static RemotingCommand headerDecode(byte[] headerData, SerializeType type) {
		return SerializerEngine.deserialize(headerData, RemotingCommand.class, type.getCode());
	}

	private byte[] headerEncode() {
		this.makeCommandCustomHeaderToExtFields();
		return SerializerEngine.serialize(this, this.serializeType.getCode());
	}
	/**
	 * 标记序列化类型 
	 * @param length
	 * @param serializeType
	 * @return
	 */
	private byte[] signSerializeType(int length,SerializeType serializeType) {
		byte[] result = new byte[4];
		result[0] = serializeType.getCode();
		result[1] = (byte)((length >> 2*8) & 0xFF);
		result[2] = (byte)((length >> 1*8) & 0xFF);
		result[3] = (byte)((length >> 0*8) & 0xFF);
		return result;
	}
	
	@Override
	protected void read() {
		this.code = this.readShort();
		this.version = this.readShort();
		this.unique = this.readInt();
		this.flag = this.readInt();
		this.remark = this.readString();
		this.extFields = this.readMap(String.class, String.class);
	}
	@Override
	protected void write() {
		this.writeShort((short)this.code);
		this.writeShort((short)this.version);
		this.writeInt(this.unique);
		this.writeInt(this.flag);
		this.writeString(this.remark);
		this.writeMap(this.extFields);
	}
}
