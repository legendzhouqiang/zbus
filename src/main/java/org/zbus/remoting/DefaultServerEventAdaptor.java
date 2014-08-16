package org.zbus.remoting;

import java.io.IOException;

import org.zbus.remoting.znet.Session;

public class DefaultServerEventAdaptor extends ServerEventAdaptor{  
    @Override
    public void onMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	if(this.globalHandler != null){
    		this.globalHandler.handleMessage(msg, sess);
    	}
    	
    	String cmd = msg.getCommand();
    	if(cmd == null){ 
    		Message res = new Message();
    		res.setMsgId(msg.getMsgId()); 
        	res.setStatus("400");
        	res.setBody("Bad format: missing command"); 
        	sess.write(res);
    		return;
    	}
    	
    	MessageHandler handler = handlerMap.get(cmd);
    	if(handler != null){
    		handler.handleMessage(msg, sess);
    		return;
    	}
    	
    	Message res = new Message();
    	res.setMsgId(msg.getMsgId()); 
    	res.setStatus("400");
    	String text = String.format("Bad format: command(%s) not support", cmd);
    	res.setBody(text); 
    	sess.write(res); 
    } 
 
}

