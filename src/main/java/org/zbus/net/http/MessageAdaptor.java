/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.net.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;

public class MessageAdaptor extends IoAdaptor{   
	protected Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<String, MessageHandler>();
	protected MessageHandler globalHandler;  

	public MessageAdaptor(){
		codec(new MessageCodec()); //个性化消息编解码
		this.registerHandler(Message.HEARTBEAT, new MessageHandler() { 
			public void handle(Message msg, Session sess) throws IOException { 
				//ignore
			}
		});
	}
	 
	public void registerHandler(String command, MessageHandler handler){
    	this.handlerMap.put(command, handler);
    }
    
    public void registerGlobalHandler(MessageHandler globalHandler) {
		this.globalHandler = globalHandler;
	}  
    
    public String findHandlerKey(Message msg){
    	String cmd = msg.getCmd();
		if(cmd == null){
			return msg.getPath();
		}
		return cmd;
    }
    
    public void onMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	if(this.globalHandler != null){
    		this.globalHandler.handle(msg, sess);
    	}
    	
    	String cmd = findHandlerKey(msg);
    	if(cmd == null){ 
    		Message res = new Message();
    		res.setId(msg.getId()); 
        	res.setStatus(400);
        	res.setBody("Bad format: missing command"); 
        	sess.write(res);
    		return;
    	}
    	
    	MessageHandler handler = handlerMap.get(cmd);
    	if(handler != null){
    		handler.handle(msg, sess);
    		return;
    	}
    	
    	Message res = new Message();
    	res.setId(msg.getId()); 
    	res.setStatus(400);
    	String text = String.format("Bad format: command(%s) not support", cmd);
    	res.setBody(text); 
    	sess.write(res); 
    } 

}

