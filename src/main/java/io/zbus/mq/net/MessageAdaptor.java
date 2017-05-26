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
package io.zbus.mq.net;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Message;
import io.zbus.net.IoAdaptor;
import io.zbus.net.Session;

public class MessageAdaptor implements IoAdaptor{    
	private static final Logger log = LoggerFactory.getLogger(MessageAdaptor.class);
	protected SessionMessageHandler filterHandler;   
	protected Map<String, SessionMessageHandler> handlerMap = new ConcurrentHashMap<String, SessionMessageHandler>(); 
	protected Map<String, Session> sessionTable;
	
	public MessageAdaptor(){ 
		this(new ConcurrentHashMap<String, Session>());
	}
	
	public MessageAdaptor(Map<String, Session> sessionTable){
		this.sessionTable = sessionTable;
		this.cmd(Message.HEARTBEAT, new SessionMessageHandler() { 
			public void handle(Message msg, Session sess) throws IOException { 
				//ignore
			}
		});
	}
	 
	public void cmd(String command, SessionMessageHandler handler){
    	this.handlerMap.put(command, handler);
    }
	 
    public void registerFilterHandler(SessionMessageHandler filterHandler) {
		this.filterHandler = filterHandler;
	}  
    
    public void sessionMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	final String msgId = msg.getId();
    	
    	if(this.filterHandler != null){
    		this.filterHandler.handle(msg, sess);
    	}
    	
    	String cmd = msg.getCommand();
    	if(cmd != null){ //cmd
    		SessionMessageHandler handler = handlerMap.get(cmd);
        	if(handler != null){
        		handler.handle(msg, sess);
        		return;
        	}
    	}
    	 
    	Message res = new Message();
    	res.setId(msgId); 
    	res.setStatus(404);
    	String text = String.format("Not Found(404): Command(%s)", cmd);
    	res.setBody(text); 
    	sess.write(res); 
    } 
     
	@Override
	public void sessionCreated(Session sess) throws IOException {
		log.info("Session Created: " + sess);
		sessionTable.put(sess.id(), sess);
	}

	@Override
	public void sessionToDestroy(Session sess) throws IOException {
		log.info("Session Destroyed: " + sess);
		cleanSession(sess);
	}
 
	@Override
	public void sessionError(Throwable e, Session sess) throws Exception { 
		log.info("Session Error: " + sess, e);
		cleanSession(sess);
	} 

	@Override
	public void sessionIdle(Session sess) throws IOException { 
		log.info("Session Idled: " + sess);
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

