package io.zbus.net.ssl;

import io.netty.handler.ssl.SslContext;
import io.zbus.net.EventLoop;
import io.zbus.net.Server;
import io.zbus.net.Ssl;
import io.zbus.net.http.HttpMessageAdaptor;
import io.zbus.net.http.HttpWsServer; 

public class SslServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		SslContext context = Ssl.buildServerSsl("ssl/zbus.crt", "ssl/zbus.key");
		
		EventLoop loop = new EventLoop(); 
		loop.setSslContext(context);
		
		Server server = new HttpWsServer(loop);  
		server.start(80, new HttpMessageAdaptor());  
	} 
}
