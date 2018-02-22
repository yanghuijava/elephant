package com.yanghui.elephant.remoting.netty;

import java.nio.ByteBuffer;

import com.yanghui.elephant.remoting.common.RemotingHelper;
import com.yanghui.elephant.remoting.common.RemotingUtil;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.log4j.Log4j2;
/**
 * 
 * @author --小灰灰--
 *
 */
@Log4j2
public class NettyDecoder extends LengthFieldBasedFrameDecoder{
	
	private static final int FRAME_MAX_LENGTH =
	        Integer.parseInt(System.getProperty("com.elephant.remoting.frameMaxLength", "16777216"));

	public NettyDecoder() {
		super(FRAME_MAX_LENGTH, 0, 4, 0, 4);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		ByteBuf byteBuf = null;
		try {
			byteBuf = (ByteBuf)super.decode(ctx, in);
			if(null == byteBuf) {
				return null;
			}
			ByteBuffer byteBuffer = byteBuf.nioBuffer();
			return RemotingCommand.decode(byteBuffer);
		}catch (Exception e) {
			log.error("反序列化发生异常, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()), e);
            RemotingUtil.closeChannel(ctx.channel());
		} finally {
			if(null != byteBuf) {
				byteBuf.release();
			}
		}
		return null;
	}
}
