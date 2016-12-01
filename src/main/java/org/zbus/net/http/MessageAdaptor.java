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

import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.net.IoAdaptor;
import org.zbus.net.Session;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.Message.MessageProcessor;

public class MessageAdaptor implements IoAdaptor{    
	private static final Logger log = LoggerFactory.getLogger(MessageAdaptor.class);
	protected MessageHandler filterHandler;   
	protected Map<String, MessageHandler> cmdHandlerMap = new ConcurrentHashMap<String, MessageHandler>();
	protected Map<String, MessageProcessor> urlHandlerMap = new ConcurrentHashMap<String, MessageProcessor>();
	protected Map<String, Session> sessionTable;
	
	public MessageAdaptor(){ 
		this(new ConcurrentHashMap<String, Session>());
	}
	
	public MessageAdaptor(Map<String, Session> sessionTable){
		this.sessionTable = sessionTable;
		this.cmd(Message.HEARTBEAT, new MessageHandler() { 
			public void handle(Message msg, Session sess) throws IOException { 
				//ignore
			}
		});
	}
	 
	public void cmd(String command, MessageHandler handler){
    	this.cmdHandlerMap.put(command, handler);
    }
	
	public void url(String path, final MessageProcessor processor){
		if(!path.startsWith("/")){
			path = "/"+path;
		}
		
		this.urlHandlerMap.put(path, processor);
		String cmd = path;
		if(cmd.startsWith("/")){
			cmd = cmd.substring(1);
		}
		cmd(cmd, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				final String msgId = msg.getId();
				Message res = processor.process(msg);
				if(res != null){
					res.setId(msgId);
					sess.write(res);
				}
			}
		});
	}
	 
    public void registerFilterHandler(MessageHandler filterHandler) {
		this.filterHandler = filterHandler;
	}  
    
    public void onSessionMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	final String msgId = msg.getId();
    	
    	if(this.filterHandler != null){
    		this.filterHandler.handle(msg, sess);
    	}
    	
    	String cmd = msg.getCmd();
    	if(cmd != null){ //cmd
    		MessageHandler handler = cmdHandlerMap.get(cmd);
        	if(handler != null){
        		handler.handle(msg, sess);
        		return;
        	}
    	}
    	
    	String path = msg.getRequestPath(); //requestPath
    	if(path == null){ 
    		Message res = new Message();
    		res.setId(msgId); 
        	res.setStatus(400);
        	res.setBody("Bad Format(400): Missing Command and RequestPath"); 
        	sess.write(res);
    		return;
    	}
    	
    	MessageProcessor urlHandler = urlHandlerMap.get(path);
    	if(urlHandler != null){
    		Message res = null; 
    		try{
    			res = urlHandler.process(msg); 
	    		if(res != null){
	    			res.setId(msgId);
	    			if(res.getStatus() == null){
	    				res.setStatus(200);// default to 200
	    			}
	    			sess.write(res);
	    		}
    		} catch (Exception e) { 
    			res = new Message();
    			res.setStatus(500);
    			res.setBody("Internal Error(500): " + e);
    			sess.write(res);
			}
    
    		return;
    	} 
    	
    	Message res = new Message();
    	res.setId(msgId); 
    	res.setStatus(404);
    	String text = String.format("Not Found(404): %s", path);
    	res.setBody(text); 
    	sess.write(res); 
    } 
     
	@Override
	public void onSessionCreated(Session sess) throws IOException {
		log.info("Session Created: " + sess);
		sessionTable.put(sess.id(), sess);
	}

	@Override
	public void onSessionToDestroy(Session sess) throws IOException {
		log.info("Session Destroyed: " + sess);
		cleanSession(sess);
	}
 
	@Override
	public void onSessionError(Throwable e, Session sess) throws Exception {
		log.info("Session Error: " + sess);
		cleanSession(sess);
	} 

	@Override
	public void onSessionIdle(Session sess) throws IOException { 
		log.info("Session Idle Remove: " + sess);
		cleanSession(sess);
	}
	
	protected void cleanSession(Session sess) throws IOException {
		try{
			sess.close();
		} finally {
			sessionTable.remove(sess.id());
		} 
	}
}

