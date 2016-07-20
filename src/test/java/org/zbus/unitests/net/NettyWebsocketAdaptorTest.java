package org.zbus.unitests.net;

import org.zbus.net.Server;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.net.http.MessageAdaptor;
import org.zbus.net.http.MessageServer;

public class NettyWebsocketAdaptorTest {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		Server server = new MessageServer();
		
		MessageAdaptor ioAdaptor = new MessageAdaptor();
		ioAdaptor.url("/hello", new MessageProcessor() {
			
			@Override
			public Message process(Message request) {
				Message res = new Message(); 
				res.setStatus(200);
				res.setBody("hello "+ System.currentTimeMillis());
				return res;
			}
		});	
		
		server.start(15555, ioAdaptor);

	}

}
