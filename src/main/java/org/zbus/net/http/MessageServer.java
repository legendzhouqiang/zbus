package org.zbus.net.http;

import java.util.List;

import org.zbus.net.CodecInitializer;
import org.zbus.net.EventDriver;
import org.zbus.net.tcp.TcpServer;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class MessageServer extends TcpServer { 
	public MessageServer() {
		this(null);
	}

	public MessageServer(EventDriver driver) {
		super(driver); 
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<Object> p) {
				p.add(new HttpServerCodec());
				p.add(new HttpObjectAggregator(1024 * 1024 * 10));
				p.add(new MessageToHttpWsCodec());
			}
		}); 
	} 
}
