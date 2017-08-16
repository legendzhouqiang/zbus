package io.zbus.performance.transport;

import java.io.IOException;

import io.zbus.transport.Session;
import io.zbus.transport.http.Message;
import io.zbus.transport.http.MessageAdaptor;
import io.zbus.transport.http.MessageHandler;
import io.zbus.transport.http.MessageServer;
import io.zbus.transport.inproc.InprocClient;

public class ServerInprocClientPerf {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		MessageAdaptor adaptor = new MessageAdaptor();
		
		adaptor.cmd("", new MessageHandler() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {  
				Message res = new Message();
				res.setId(msg.getId()); //match the ID for response
				res.setStatus(200);
				res.setBody("Hello world");
				session.write(res);
			}
		});

		MessageServer server = new MessageServer();   
		server.start(80, adaptor);  
		
		
		InprocClient<Message , Message> client = new InprocClient<Message, Message>(adaptor);
		
		int N = 1000000;
		long start = System.currentTimeMillis();
		for(int i=0;i<N;i++){
			Message req = new Message(); 
			client.invokeSync(req); 
		}
		long end = System.currentTimeMillis();
		System.out.println(N*1000.0/(end-start));
		
		client.close();
		server.stop();
	} 
}
