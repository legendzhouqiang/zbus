package org.zbus.remoting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.remoting.znet.EventAdaptor;

public abstract class ServerEventAdaptor extends EventAdaptor{  
	protected Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<String, MessageHandler>();
	
	protected MessageHandler globalHandler;
   
	public void registerHandler(String command, MessageHandler handler){
    	this.handlerMap.put(command, handler);
    }
    
    public void registerGlobalHandler(MessageHandler beforeHandler) {
		this.globalHandler = beforeHandler;
	}  
}

