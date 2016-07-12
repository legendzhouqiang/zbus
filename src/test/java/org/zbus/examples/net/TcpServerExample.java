package org.zbus.examples.net;

import java.io.IOException;
import java.util.List;

import org.zbus.net.CodecInitializer;
import org.zbus.net.IoAdaptor;
import org.zbus.net.Server;
import org.zbus.net.Session;
import org.zbus.net.http.MessageToHttpWsCodec;
import org.zbus.net.tcp.TcpServer;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class TcpServerExample {
	
	public static void main(String[] args) throws Exception {   
		 
		Server server = new TcpServer(); 
		server.codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpServerCodec());
				p.add(new HttpObjectAggregator(1024 * 1024 * 32));
				p.add(new MessageToHttpWsCodec());
			}
		}); 
		
		server.start(8080, new IoAdaptor() {
			
			@Override
			public void onSessionToDestroy(Session sess) throws IOException { 
				
			}
			
			@Override
			public void onSessionMessage(Object msg, Session sess) throws IOException { 
				System.out.println(msg);
			}
			
			@Override
			public void onSessionError(Throwable e, Session sess) throws Exception { 
			}
			
			@Override
			public void onSessionCreated(Session sess) throws IOException {
			}
		});
		 
		server.join();
		server.close();
	}

}
