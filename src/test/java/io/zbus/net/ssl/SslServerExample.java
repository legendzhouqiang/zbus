package io.zbus.net.ssl;

import io.netty.handler.ssl.SslContext;
import io.zbus.transport.Server;
import io.zbus.transport.Ssl;
import io.zbus.transport.http.HttpWsServer;
import io.zbus.transport.http.HttpWsServerAdaptor; 

public class SslServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		SslContext context = Ssl.buildServerSsl("ssl/zbus.crt", "ssl/zbus.key"); 
		
		Server server = new HttpWsServer(); 
		server.setSslContext(context);
		server.start(80, new HttpWsServerAdaptor());  
	} 
}
