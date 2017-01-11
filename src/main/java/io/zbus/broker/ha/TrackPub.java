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
package io.zbus.broker.ha;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.zbus.kit.log.Logger;
import io.zbus.kit.log.LoggerFactory;
import io.zbus.mq.Protocol;
import io.zbus.net.EventDriver;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.net.http.Message;
import io.zbus.net.http.MessageClient;

public class TrackPub implements Closeable{  
	private static final Logger log = LoggerFactory.getLogger(TrackPub.class);
	
	private Set<MessageClient> allTrackers = new HashSet<MessageClient>();
	private Set<MessageClient> healthyTrackers = new HashSet<MessageClient>();
	private ConnectedHandler connectedHandler;
	private EventDriver eventDriver;
	
	public TrackPub(String trackServerList, EventDriver driver) {
		
		if(driver == null){
			throw new IllegalArgumentException("EventDriver can not be null");
		}
		if(trackServerList == null){
			throw new IllegalArgumentException("trackServerList can not be null");
		}
		this.eventDriver = driver;
		String[] blocks = trackServerList.split("[;]");
    	for(String block : blocks){
    		final String address = block.trim();
    		if(address.equals("")) continue;
    		 
    		final MessageClient client = new MessageClient(address, eventDriver);
    		allTrackers.add(client);
    		 
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
    				healthyTrackers.remove(client); 
    				client.ensureConnectedAsync(); 
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected() throws IOException {  
    				log.info("TrackServer(%s) connected", address);
    				healthyTrackers.add(client); 
    				if(connectedHandler != null){
    					connectedHandler.onConnected();
    				}
    			}
			}); 
    	} 
    }
	
	public void start(){
		for(final MessageClient client : allTrackers){
			client.ensureConnectedAsync();
		}
	}
	
	public void sendToAllTrackers(Message msg){
		msg.removeHead(Protocol.ID);
		for(MessageClient client : healthyTrackers){ 
			try{ 
				if(client.hasConnected()){
					client.sendMessage(msg);
				}
			} catch (IOException e){
				log.error(e.getMessage(), e);
			} catch (InterruptedException e) {
				break;
			}
    	}
	}
	
	public void pubServerJoin(String targetServerAddr){
		Message msg = new Message();
		msg.setCmd(HaCommand.ServerJoin);
		msg.setServer(targetServerAddr); 
		sendToAllTrackers(msg);
	}
	
	public void pubServerLeave(String targetServerAddr){
		Message msg = new Message();
		msg.setCmd(HaCommand.ServerLeave);
		msg.setServer(targetServerAddr);
		sendToAllTrackers(msg);
	}
	
	public void pubEntryUpdate(ServerEntry se){ 
		Message msg = new Message();
		msg.setBody(se.pack());
		msg.setCmd(HaCommand.EntryUpdate);  
		sendToAllTrackers(msg);
	} 
	
	public void pubEntryRemove(String entryId){ 
		Message msg = new Message();
		msg.setBody(entryId);
		msg.setCmd(HaCommand.EntryRemove);  
		sendToAllTrackers(msg);
	} 
	
	public void onConnected(ConnectedHandler connectedHandler){
		this.connectedHandler = connectedHandler;
	}
	
    @Override
    public void close() throws IOException { 
    	for(MessageClient client : allTrackers){
    		client.close();
    	}
    	allTrackers.clear();
    	healthyTrackers.clear(); 
    }
}
