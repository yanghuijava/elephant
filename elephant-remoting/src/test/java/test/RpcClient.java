package test;

import java.net.InetSocketAddress;

import com.yanghui.elephant.common.constant.LocalTransactionState;
import com.yanghui.elephant.common.message.Message;
import com.yanghui.elephant.remoting.netty.NettyDecoder;
import com.yanghui.elephant.remoting.netty.NettyEncoder;
import com.yanghui.elephant.remoting.procotol.RemotingCommand;
import com.yanghui.elephant.remoting.procotol.RemotingCommandType;
import com.yanghui.elephant.remoting.procotol.SerializeType;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class RpcClient {
	
	private final String host;
	private final int port;
	
	private EventLoopGroup group;
	private Bootstrap b;
	private ChannelFuture f;
	
	private static class SingletonHolder {
		static final RpcClient instance = new RpcClient("127.0.0.1",8888);
	}
	
	public static RpcClient getInstance(){
		return SingletonHolder.instance;
	}
	
	private RpcClient(String host,int port){
		this.host = host;
		this.port = port;
		group = new NioEventLoopGroup();
		b = new Bootstrap();
		b.group(group)
			.channel(NioSocketChannel.class)
			.remoteAddress(new InetSocketAddress(this.host,this.port))
			.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel sc)
						throws Exception {
					sc.pipeline().addLast(new NettyEncoder(SerializeType.HESSIAN));
					sc.pipeline().addLast(new NettyDecoder(RemotingCommand.class,SerializeType.HESSIAN));
					sc.pipeline().addLast(new IdleStateHandler(0, 0, 120));
				}
			});
	}
	
	public void connect() throws InterruptedException{
		this.f = this.b.connect().sync();
		System.out.println("远程服务器已经连接, 可以进行数据交换......");
	}
	
	public void shutdown(){
		this.group.shutdownGracefully();
	}
	
	public ChannelFuture getChannelFuture() throws InterruptedException{
		if(this.f == null){
			this.connect();
		}
		if(!this.f.channel().isActive()){
			this.connect();
		}
		return f;
	}
	
	public static void main(String[] args) throws InterruptedException {
		RpcClient rpcClient = RpcClient.getInstance();
		RemotingCommand cmd = new RemotingCommand();
		cmd.setType(RemotingCommandType.REQUEST_COMMAND);
		cmd.setBody(new Message("queue://test","我是消息".getBytes()));
		cmd.setLocalTransactionState(LocalTransactionState.PRE_MESSAGE);
		try {
			rpcClient.getChannelFuture().channel().writeAndFlush(cmd);
		} finally{
			rpcClient.shutdown();
		}
	}
}
