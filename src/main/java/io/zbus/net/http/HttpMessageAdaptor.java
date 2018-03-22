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

public class HttpMessageAdaptor extends ServerAdaptor{     
	protected SessionMessageHandler<HttpMessage> filterHandler;   
	protected Map<String, SessionMessageHandler<HttpMessage>> handlerMap = new ConcurrentHashMap<>();  
	
	public HttpMessageAdaptor(){ 
		this(null);
	}
	
	public HttpMessageAdaptor(Map<String, Session> sessionTable){
		super(sessionTable); 
	} 
	
	public void url(String url, SessionMessageHandler<HttpMessage> handler){ 
    	this.handlerMap.put(url, handler);
    }
	 
    public void registerFilterHandler(SessionMessageHandler<HttpMessage> filterHandler) {
		this.filterHandler = filterHandler;
	}  
    
    public void onMessage(Object obj, Session sess) throws IOException {  
    	HttpMessage msg = (HttpMessage)obj;  
    	final String msgId = msg.getId();
    	handleUrlMessage(msg);
    	
    	if(this.filterHandler != null){
    		this.filterHandler.handle(msg, sess);
    	}
    	
    	String url = msg.getUrl();
    	SessionMessageHandler<HttpMessage> handler = handlerMap.get(url);
    	if(handler != null){
    		handler.handle(msg, sess);
    		return;
    	}  
    	
    	HttpMessage res = new HttpMessage();
    	res.setId(msgId); 
    	res.setStatus(404);
    	String text = String.format("404: %s Not Found", url);
    	res.setBody(text); 
    	sess.write(res); 
    }  
    
    protected void handleUrlMessage(HttpMessage msg){ 
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
