package io.zbus.net;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

public class EventLoop implements Closeable { 
	private EventLoopGroup group;    
	
	private SslContext sslContext; 
	private int idleTimeInSeconds = 180; //180s 
	private int packageSizeLimit = 1024*1024*1024; //maximum of 1G
	
	private Map<String, SslContext> sslContextCache = new ConcurrentHashMap<>();

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
	
	public SSLEngine buildSSLEngine(String host, int port, ByteBufAllocator alloc){
		String key = String.format("%s:%d", host,port);
		SslContext sslContext = sslContextCache.get(key); 
		if(sslContext == null){
			sslContext = buildSslContext();
			sslContextCache.put(key, sslContext);
		}
		
		SSLEngine sslEngine = sslContext.newEngine(alloc, host, port); 
		sslEngine.setUseClientMode(true);
		SSLParameters params = sslEngine.getSSLParameters();
		params.setEndpointIdentificationAlgorithm("HTTPS");
		sslEngine.setSSLParameters(params);
		return sslEngine; 
	}
	
	private SslContext buildSslContext() { 
		try {
			SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
					.sslProvider(SslProvider.JDK)
					.sessionCacheSize(0)
					.sessionTimeout(0);
			String[] protocols = new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" };
			sslContextBuilder.protocols(protocols);
			SslContext sslContext = sslContextBuilder.build();
			return sslContext;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	} 
}
