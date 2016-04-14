package org.zbus.net.netty;
 
import java.util.ArrayList;
import java.util.List;

import org.zbus.net.ClientAdaptor;
import org.zbus.net.EventDriver;
import org.zbus.net.Sync.Id;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;


public class NettyClient<REQ extends Id, RES extends Id> extends ClientAdaptor<REQ, RES> {
	protected Bootstrap bootstrap;
	protected final EventLoopGroup group;  
	protected SslContext sslCtx;
	protected ChannelFuture channelFuture; 
	
	public NettyClient(String address, EventDriver driver){
		super(address);
		driver.validateNetty(); 
		group = (EventLoopGroup)driver.getGroup();
		sslCtx = (SslContext)driver.getSslContext();
	}
	
	public synchronized void connectAsync(){  
		init(); 
		
		channelFuture = bootstrap.connect(host, port);
	}   
	
	private void init(){
		if(bootstrap != null) return;
		
		bootstrap = new Bootstrap();
		bootstrap.group(this.group) 
		 .channel(NioSocketChannel.class)  
		 .handler(new ChannelInitializer<SocketChannel>() { 
			NettyToIoAdaptor nettyToIoAdaptor = new NettyToIoAdaptor(NettyClient.this);
			@Override
			protected void initChannel(SocketChannel ch) throws Exception { 
				if(codecInitializer == null){
					throw new IllegalStateException("Missing codecInitializer");
				}
				
				ChannelPipeline p = ch.pipeline();
				if(sslCtx != null){
					p.addLast(sslCtx.newHandler(ch.alloc()));
				}
				if(codecInitializer != null){
					List<Object> handlers = new ArrayList<Object>();
					codecInitializer.initPipeline(handlers);
					for(Object handler : handlers){
						if(!(handler instanceof ChannelHandler)){
							throw new IllegalArgumentException("Invalid ChannelHandler: " + handler);
						} 
						p.addLast((ChannelHandler)handler);
					}
				}
				p.addLast(nettyToIoAdaptor);
			}
		});  
	} 
	 
}
