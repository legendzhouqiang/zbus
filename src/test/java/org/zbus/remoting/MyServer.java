package org.zbus.remoting;

import java.io.IOException;

import org.zbus.remoting.nio.Session;


public class MyServer{
 
	public static void main(String[] args) throws Exception { 
		
		ServerDispatcherManager dispatcherManager = new ServerDispatcherManager();
		
		dispatcherManager.registerGlobalHandler(new MessageHandler() {
			
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {
				//System.out.println(msg);
			}
		});
		
		dispatcherManager.registerHandler("test", new MessageHandler() {
			
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {
				Message res = new Message();
				res.setStatus("200");
				res.setMsgId(msg.getMsgId());
				res.setBody(System.currentTimeMillis()+"");
				sess.write(res);
			}
			
		});
		
		RemotingServer server = new RemotingServer(80, dispatcherManager);
		server.start();
	}

}
