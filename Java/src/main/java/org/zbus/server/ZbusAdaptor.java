package org.zbus.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.net.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageAdaptor;
import org.zbus.net.http.Message.MessageHandler;

public class ZbusAdaptor extends MessageAdaptor implements Closeable {
	private final Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<String, MessageHandler>();
	private final String serverAddress;
	
	public ZbusAdaptor(String serverAddress){
		this.serverAddress = serverAddress;
	} 
	
	public void onSessionMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	msg.setSender(sess.id());
		msg.setServer(serverAddress); 
		msg.setRemoteAddr(sess.getRemoteAddress());  
		
		String cmd = msg.getCmd();  
    	if(cmd != null){
	    	MessageHandler handler = handlerMap.get(cmd);
	    	if(handler != null){
	    		handler.handle(msg, sess);
	    		return;
	    	}
    	}
    	
    	Message res = new Message();
    	res.setId(msg.getId()); 
    	res.setStatus(400);
    	String text = String.format("Bad format: command(%s) not support", cmd);
    	res.setBody(text); 
    	sess.write(res); 
    } 
	
	@Override
	public void close() throws IOException { 
		
	} 
}