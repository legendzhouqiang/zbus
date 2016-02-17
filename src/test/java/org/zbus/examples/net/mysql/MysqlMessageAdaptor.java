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
package org.zbus.examples.net.mysql;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.examples.net.mysql.MysqlMessage.MysqlMessageHandler;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;

public class MysqlMessageAdaptor extends IoAdaptor{   
	protected Map<String, MysqlMessageHandler> cmdHandlerMap = new ConcurrentHashMap<String, MysqlMessageHandler>();
	public MysqlMessageAdaptor(){
		codec(new MysqlMessageCodec());  
	}
	
	public void cmd(String path, final MysqlMessageHandler messageHandler){
		this.cmdHandlerMap.put(path, messageHandler);
	} 
    
    @Override
    protected void onSessionAccepted(Session sess) throws IOException {
    	super.onSessionAccepted(sess);
    	System.out.println("onAccpet"); 
    	
    	byte[] buf = new byte[]{10,53,46,53,46,51,51,97,45,77,97,114,105,97,68,66,45,108,111,103,0,10,0,0,0,79,102,71,85,106,66,87,49,0,-1,-9,33,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,89,121,78,55,51,86,80,68,80,80,106,117,0};
    	
    	MysqlMessage msg = new MysqlMessage();
    	msg.data = buf;
    	msg.pakgId=0;
    	msg.length = buf.length;
    	System.out.println("msg.length "+msg.length);
    	sess.write(msg);
    }
    public void onMessage(Object obj, Session sess) throws IOException {  
    	System.out.println(obj);
    	MysqlMessage msg = (MysqlMessage)obj;  
    	
    	String cmd = "1";//msg.getCmd();
    	if(cmd != null){ //cmd
    		MysqlMessageHandler handler = cmdHandlerMap.get(cmd);
        	if(handler != null){
        		handler.handle(msg, sess);
        		return;
        	}
    	} 
    }  
}

