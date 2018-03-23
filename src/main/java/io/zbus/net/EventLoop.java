package io.zbus.net;

import java.io.Closeable;
import java.io.IOException;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;

public class EventLoop implements Closeable { 
	private EventLoopGroup group;    
	
	private SslContext sslContext; 
	private int idleTimeInSeconds = 180; //180s 
	private int packageSizeLimit = 1024*1024*1024; //maximum of 1G
	

	public EventLoop() {
		try {
			group = new NioEventLoopGroup();  
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}  
	
	public EventLoopGroup getGroup() { 
		return group; 
	}

	public SslContext getSslContext() {
		return sslContext;
	}
	
	public boolean isSslEnabled() {
		return sslContext != null;
	} 

	public void setSslContext(SslContext sslContext) { 
		this.sslContext = sslContext;
	} 
	 
	@Override
	public void close() throws IOException {
		if (group != null) {
			group.shutdownGracefully(); 
			group = null;
		} 
	}

	public int getIdleTimeInSeconds() {
		return idleTimeInSeconds;
	}

	public void setIdleTimeInSeconds(int idleTimeInSeconds) {
		this.idleTimeInSeconds = idleTimeInSeconds;
	}

	public int getPackageSizeLimit() {
		return packageSizeLimit;
	}

	public void setPackageSizeLimit(int packageSizeLimit) {
		this.packageSizeLimit = packageSizeLimit;
	}   

}
