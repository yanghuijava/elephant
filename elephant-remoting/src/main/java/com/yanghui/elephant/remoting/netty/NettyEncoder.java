package com.yanghui.elephant.remoting.netty;

import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import com.yanghui.elephant.remoting.procotol.SerializeType;
import com.yanghui.elephant.remoting.procotol.SerializerEngine;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {

	private SerializeType serializeType;

    public NettyEncoder(SerializeType serializeType) {
        this.serializeType = serializeType;
    }
	
	@Override
	protected void encode(ChannelHandlerContext ctx, RemotingCommand msg,ByteBuf out) throws Exception {
		//将对象序列化为字节数组
        byte[] data = SerializerEngine.serialize(msg, serializeType.getCode());
        //将字节数组(消息体)的长度作为消息头写入,解决半包/粘包问题
        out.writeInt(data.length);
        //写入序列化后得到的字节数组
        out.writeBytes(data);
	}
}
