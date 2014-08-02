package org.zbus.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.remoting.Message;
import org.remoting.MessageHandler;
import org.zbus.server.mq.ReplyHelper;
import org.znet.Session;


public class AdminHandler implements MessageHandler {  
	private String adminToken = ""; 
	private Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<String, MessageHandler>();

	
	public void registerHandler(String command, MessageHandler handler){
    	this.handlerMap.put(command, handler);
    }
	
	@Override
	public void handleMessage(Message msg, Session sess) throws IOException {
		if(!adminToken.equals("") && !adminToken.equals(msg.getToken())){
    		ReplyHelper.reply403(msg, sess);
    		return;
    	}
		String cmd = msg.getHeadOrParam("cmd", "");
		MessageHandler handler = this.handlerMap.get(cmd);
		if(handler == null){  
			msg.setBody("Admin cmd=%s Not Found", cmd);
			ReplyHelper.reply400(msg, sess);
    		return; 
		} 
		handler.handleMessage(msg, sess);
	}

	public String getAdminToken() {
		return adminToken;
	}

	public void setAdminToken(String adminToken) {
		this.adminToken = adminToken;
	} 
	
	
}
