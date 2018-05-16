package io.zbus.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler; 

public class Server implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(Server.class); 
	
	protected CodecInitializer codecInitializer; 
	protected EventLoop loop;
	protected boolean ownLoop;  
	//Port ==> Server IoAdaptor
	protected Map<Integer, ServerInfo> listenTable = new ConcurrentHashMap<Integer, ServerInfo>();
  
	public Server(){
		this(null); 
	}
	
	public Server(EventLoop loop){ 
		this.loop = loop; 
		if (this.loop == null) {
			this.loop = new EventLoop();
			this.ownLoop = true;
		} else {
			this.ownLoop = false;
		} 
	}   
 
	public void start(int port, IoAdaptor ioAdaptor) {
		start("0.0.0.0", port, ioAdaptor, true);
	}
 
	public void start(final String host, final int port, IoAdaptor ioAdaptor){
		start(host, port, ioAdaptor, true);
	} 
	
	public void start(final String host, final int port, IoAdaptor ioAdaptor, boolean isDefault) {
		EventLoopGroup bossGroup = loop.getGroup();
		EventLoopGroup workerGroup = loop.getGroup();  
		
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
		 .option(ChannelOption.SO_BACKLOG, 102400) //TODO make it configurable
		 .channel(NioServerSocketChannel.class) 
		 .handler(new LoggingHandler(LogLevel.DEBUG))
		 .childHandler(new SocketChannelInitializer(ioAdaptor));
		
		ServerInfo info = new ServerInfo(); 
		info.bootstrap = b;
		info.serverChanneFuture = b.bind(host, port).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()){
					if(loop.isSslEnabled()){
						log.info("Server([SSL]{}:{}) started", host, port);
					} else {
						log.info("Server({}:{}) started", host, port);
					} 
				} else { 
					String message = String.format("Server({}:{}) failed to start", host, port);
					throw new IOException(message, future.cause());
				}
			}
		}); 
		listenTable.put(port, info); 
	} 
	
	@Override
	public void close() throws IOException {
		if(ownLoop && loop != null){
			loop.close(); 
			loop = null;
		}
	} 
	 
	public int getRealPort(int bindPort) throws InterruptedException{
		if(!listenTable.containsKey(bindPort)){
			return -1; //indicates not found;
		}
		ServerInfo e = listenTable.get(bindPort);
		SocketAddress addr = e.serverChanneFuture.await().channel().localAddress();
		return ((InetSocketAddress)addr).getPort();
	}
	 
	public EventLoop getEventLoop() {
		return this.loop;
	}
	 
	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	}  
	
	static class ServerInfo{
		ServerBootstrap bootstrap;
		ChannelFuture serverChanneFuture;
	}  
	
	class SocketChannelInitializer extends ChannelInitializer<SocketChannel>{ 
		private NettyAdaptor nettyToIoAdaptor;
		private CodecInitializer codecInitializer;
		
		public SocketChannelInitializer(IoAdaptor ioAdaptor){
			this(ioAdaptor, null);
		}
		public SocketChannelInitializer(IoAdaptor ioAdaptor, CodecInitializer codecInitializer){ 
			this.nettyToIoAdaptor = new NettyAdaptor(ioAdaptor);
			this.codecInitializer = codecInitializer;
		}
		
		private CodecInitializer getCodecInitializer(){
			if(this.codecInitializer != null) return this.codecInitializer;
			return Server.this.codecInitializer;
		}
		
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {  
			ChannelPipeline p = ch.pipeline(); 
			int timeout = loop.getIdleTimeInSeconds();
			p.addLast(new IdleStateHandler(0, 0, timeout));
			SslContext sslCtx = loop.getSslContext();
			if(sslCtx != null){
				p.addLast(sslCtx.newHandler(ch.alloc()));
			}
			CodecInitializer initializer = getCodecInitializer();
			if(initializer != null){
				List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();
				initializer.initPipeline(handlers);
				for(ChannelHandler handler : handlers){
					 p.addLast((ChannelHandler)handler); 
				}
			}	 
			p.addLast(this.nettyToIoAdaptor);
		} 
	}
	 
}
