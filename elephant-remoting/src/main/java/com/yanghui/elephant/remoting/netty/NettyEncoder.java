package com.yanghui.elephant.remoting.netty;

import com.yanghui.elephant.remoting.common.RemotingHelper;
import com.yanghui.elephant.remoting.common.RemotingUtil;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.log4j.Log4j2;
/**
 * 
 * @author --小灰灰--
 *
 */
@Log4j2
public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {

	@Override
	protected void encode(ChannelHandlerContext ctx, RemotingCommand cmd,ByteBuf out) throws Exception {
		try {
			byte[] bytes = cmd.encode();
			out.writeBytes(bytes);
		} catch (Exception e) {
			log.error("序列化发生异常, "+ RemotingHelper.parseChannelRemoteAddr(ctx.channel()), e);
			if(null != null) {
				log.error(cmd.toString());
			}
			RemotingUtil.closeChannel(ctx.channel());
		}
	}
}
