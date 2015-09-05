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
package org.zbus.broker.ha;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.kit.log.Logger;
import org.zbus.net.Client.ConnectedHandler;
import org.zbus.net.Client.ErrorHandler;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageClient;

public class TrackSub implements Closeable{  
	private static final Logger log = Logger.getLogger(TrackSub.class);
	
	private Set<MessageClient> allTrackers = new HashSet<MessageClient>();
	private Set<MessageClient> healthyTrackers = new HashSet<MessageClient>();
	private int reconnectTimeout = 3000; //ms
	
	private Map<String, MessageHandler> cmdHandlers = new ConcurrentHashMap<String, MessageHandler>();
	private boolean verbose = false;
	public TrackSub(String trackServerList, Dispatcher dispatcher) {
		String[] blocks = trackServerList.split("[;]");
    	for(String block : blocks){
    		final String address = block.trim();
    		if(address.equals("")) continue;
    		
    		final MessageClient client = new MessageClient(address, dispatcher); 
    		allTrackers.add(client);
    		
    		client.onError(new ErrorHandler() {
    			@Override
    			public void onError(IOException e, Session sess)
    					throws IOException { 
    				healthyTrackers.remove(client);
    				if(verbose){
    					log.info("TrackServer(%s) down, try to reconnect in %d seconds", address, reconnectTimeout/1000);
    				}
    				try { Thread.sleep(reconnectTimeout); } catch (InterruptedException ex) { }
    				client.connectAsync();
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected(Session sess) throws IOException {  
    				log.info("TrackServer(%s) connected", address);
    				healthyTrackers.add(client); 
    				
    				clientSubAll(client);
    			}
			}); 
    		
    		client.onMessage(new MessageHandler() { 
				@Override
				public void handle(Message msg, Session sess) throws IOException { 
					log.debug("%s", msg);
					String cmd = msg.getCmd();
			    	if(cmd != null){ //cmd
			    		MessageHandler handler = cmdHandlers.get(cmd);
			        	if(handler != null){
			        		handler.handle(msg, sess);
			        		return;
			        	}
			    	}
			    	log.warn("Missing handler for command(%s)\n%s", cmd, msg);
				}
			});
    	}  
    }
	
	public void cmd(String command, MessageHandler handler){
    	this.cmdHandlers.put(command, handler);
    }
	
	public void start(){
		for(MessageClient client : this.allTrackers){
			try {
				client.connectAsync();
			} catch (IOException e) { 
				log.error(e.getMessage(), e);
			}
		}
	}
	
	private void clientSubAll(MessageClient client){
		try { 
    		Message msg = new Message(); 
    		msg.setCmd(HaCommand.SubAll);
    		client.send(msg);
		} catch (IOException e) { 
			//log.error(e.getMessage(), e);
		}
	}  
	
    @Override
    public void close() throws IOException { 
    	for(MessageClient client : allTrackers){
    		client.close();
    	}
    	allTrackers.clear();
    	healthyTrackers.clear();
    }

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	} 
    
}
