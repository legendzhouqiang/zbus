package org.zbus.net.http;

import java.util.List;

import org.zbus.net.CodecInitializer;
import org.zbus.net.IoDriver;
import org.zbus.net.tcp.TcpServer;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class MessageServer extends TcpServer { 
	public MessageServer() {
		this(null);
	}

	public MessageServer(final IoDriver driver) {
		super(driver); 
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpServerCodec());
				p.add(new HttpObjectAggregator(driver.getPackageSizeLimit()));
				p.add(new MessageToHttpWsCodec());
			}
		}); 
	} 
}
