package org.zbus.net.netty;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.zbus.net.Session;

import io.netty.channel.ChannelHandlerContext;

public class NettySession implements Session {
	private ChannelHandlerContext ctx;
	private final String id;
	private ConcurrentMap<String, Object> attributes = null;
	
	public NettySession(ChannelHandlerContext ctx) {
		this.ctx = ctx;
		this.id = UUID.randomUUID().toString();
	}
	
	@Override
	public String id() {
		return id;
	}
	
	public String getRemoteAddress() { 
		ctx.isRemoved();
		return ctx.channel().remoteAddress().toString();
	}
	
	public String getLocalAddress() { 
		return ctx.channel().localAddress().toString();
	}
	
	public void write(Object msg){
		ctx.writeAndFlush(msg);
	}
	
	public void writeAndFlush(Object msg){
		ctx.writeAndFlush(msg);
	}
	
	@Override
	public void flush() {
		ctx.flush();
	}
	
	@Override
	public void close() throws IOException {
		ctx.close();
	}
	
	@Override
	public boolean isActive() {
		return ctx.channel().isActive();
	}
	
	@Override
	public void asyncClose() throws IOException {
		close();
	}
	
	@SuppressWarnings("unchecked")
	public <V> V attr(String key) {
		if (this.attributes == null) {
			return null;
		}

		return (V) this.attributes.get(key);
	}

	public <V> void attr(String key, V value) {
		if (this.attributes == null) {
			synchronized (this) {
				if (this.attributes == null) {
					this.attributes = new ConcurrentHashMap<String, Object>();
				}
			}
		}
		this.attributes.put(key, value);
	}
	
	@Override
	public String toString() { 
		return "NettySession ["
				+ "remote=" + getRemoteAddress()
				+ ", active=" + isActive()   
				+ "]"; 
	}
}
