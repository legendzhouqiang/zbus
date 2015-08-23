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
	//全局处理器，优先级最高
	protected MessageHandler filterHandler;  
	//根据cmd处理，优先级次之
	protected Map<String, MessageHandler> cmdHandlerMap = new ConcurrentHashMap<String, MessageHandler>();
	//根据Uri处理，优先级最低
	protected Map<String, MessageProcessor> uriHandlerMap = new ConcurrentHashMap<String, MessageProcessor>();


	public MessageAdaptor(){
		codec(new MessageCodec()); //个性化消息编解码
		this.cmd(Message.HEARTBEAT, new MessageHandler() { 
			public void handle(Message msg, Session sess) throws IOException { 
				//ignore
			}
		});
	}
	 
	public void cmd(String command, MessageHandler handler){
    	this.cmdHandlerMap.put(command, handler);
    }
	
	public void uri(String path, final MessageProcessor processor){
		this.uriHandlerMap.put(path, processor);
		cmd(path, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				Message res = processor.process(msg);
				if(res != null){
					sess.write(res);
				}
			}
		});
	}
    
    public void registerFilterHandler(MessageHandler filterHandler) {
		this.filterHandler = filterHandler;
	}  
    
    public void onMessage(Object obj, Session sess) throws IOException {  
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
        	res.setResponseStatus(400);
        	res.setBody("Bad Format(400): Missing Command and RequestPath"); 
        	sess.write(res);
    		return;
    	}
    	
    	MessageProcessor uriHandler = uriHandlerMap.get(path);
    	if(uriHandler != null){
    		Message res = null; 
    		try{
    			res = uriHandler.process(msg); 
	    		if(res != null){
	    			res.setId(msgId);
	    			if(res.getResponseStatus() == null){
	    				res.setResponseStatus(200);// default to 200
	    			}
	    			sess.write(res);
	    		}
    		} catch (IOException e){ 
    			throw e;
    		} catch (Exception e) { 
    			res = new Message();
    			res.setResponseStatus(500);
    			res.setBody("Internal Error(500): " + e);
    			sess.write(res);
			}
    
    		return;
    	} 
    	
    	Message res = new Message();
    	res.setId(msgId); 
    	res.setResponseStatus(404);
    	String text = String.format("Not Found(404): %s", path);
    	res.setBody(text); 
    	sess.write(res); 
    } 

}

