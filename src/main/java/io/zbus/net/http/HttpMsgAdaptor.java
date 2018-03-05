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
package io.zbus.net.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.net.SessionMessageHandler;
import io.zbus.net.ServerAdaptor;
import io.zbus.net.Session; 

public class HttpMsgAdaptor extends ServerAdaptor{     
	protected SessionMessageHandler<HttpMsg> filterHandler;   
	protected Map<String, SessionMessageHandler<HttpMsg>> handlerMap = new ConcurrentHashMap<String, SessionMessageHandler<HttpMsg>>();  
	
	public HttpMsgAdaptor(){ 
		this(null);
	}
	
	public HttpMsgAdaptor(Map<String, Session> sessionTable){
		super(sessionTable);
		this.cmd(HttpMsg.HEARTBEAT, new SessionMessageHandler<HttpMsg>() { 
			public void handle(HttpMsg msg, Session sess) throws IOException { 
				//ignore
			}
		});
	}
	 
	public void cmd(String command, SessionMessageHandler<HttpMsg> handler){
    	this.handlerMap.put(command, handler);
    }
	
	public void url(String url, SessionMessageHandler<HttpMsg> handler){
		if(url.startsWith("/")){
			url = url.substring(1);
		}
    	this.handlerMap.put(url, handler);
    }
	 
    public void registerFilterHandler(SessionMessageHandler<HttpMsg> filterHandler) {
		this.filterHandler = filterHandler;
	}  
    
    public void onMessage(Object obj, Session sess) throws IOException {  
    	HttpMsg msg = (HttpMsg)obj;  
    	final String msgId = msg.getId();
    	handleUrlMessage(msg);
    	
    	if(this.filterHandler != null){
    		this.filterHandler.handle(msg, sess);
    	}
    	
    	String cmd = msg.getCommand();
    	if(cmd != null){ //cmd
    		SessionMessageHandler<HttpMsg> handler = handlerMap.get(cmd);
        	if(handler != null){
        		handler.handle(msg, sess);
        		return;
        	}
    	}
    	 
    	HttpMsg res = new HttpMsg();
    	res.setId(msgId); 
    	res.setStatus(404);
    	String text = String.format("404: Command(%s) Not Found", cmd);
    	res.setBody(text); 
    	sess.write(res); 
    }  
    
    protected void handleUrlMessage(HttpMsg msg){ 
    	if(msg.getCommand() != null){
    		return;
    	} 
    	String url = msg.getUrl(); 
    	if(url == null || "/".equals(url)){
    		msg.setCommand("");
    		return;
    	} 
    	int idx = url.indexOf('?');
    	String cmd = "";
    	if(idx >= 0){
    		cmd = url.substring(1, idx);  
    	} else {
    		cmd = url.substring(1);
    	} 
    	  
    	msg.setCommand(cmd.toLowerCase());  
	}
}
