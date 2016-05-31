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
package org.zbus.ha;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.kit.log.Logger;
import org.zbus.net.Client.ConnectedHandler;
import org.zbus.net.Client.DisconnectedHandler;
import org.zbus.net.EventDriver;
import org.zbus.net.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageClient;

public class TrackSub implements Closeable{  
	private static final Logger log = Logger.getLogger(TrackSub.class);
	
	private Set<MessageClient> allTrackers = new HashSet<MessageClient>();
	private Set<MessageClient> healthyTrackers = new HashSet<MessageClient>();
	
	private Map<String, MessageHandler> cmdHandlers = new ConcurrentHashMap<String, MessageHandler>();
	
	private EventDriver eventDriver;
	
	public TrackSub(String trackServerList, EventDriver driver) {
		if(driver == null){
			throw new IllegalArgumentException("EventDriver can not be null");
		}
		if(trackServerList == null){
			throw new IllegalArgumentException("trackServerList can not be null");
		}
		
		this.eventDriver = driver;
		String[] blocks = trackServerList.split("[;, ]");
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
    	
    	
    	initDefaultHandlers();
    }
	
	private void initDefaultHandlers(){
		cmd(HaCommand.ServerJoin, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String serverAddr = msg.getServer();
				if(serverJoinHandler != null){
					serverJoinHandler.onServerJoin(serverAddr);
				}
			}
		});
		
		cmd(HaCommand.ServerLeave, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String serverAddr = msg.getServer();
				if(serverLeaveHandler != null){
					serverLeaveHandler.onServerLeave(serverAddr);
				}
			}
		}); 
		
		cmd(HaCommand.EntryUpdate, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {  
				ServerEntry entry = null;
				try{
					entry = ServerEntry.unpack(msg.getBodyString());
				} catch(Exception e){
					log.error(e.getMessage(), e);
					return;
				}
				if(entryUpdateHandler != null){
					entryUpdateHandler.onEntryUpdate(entry);
				}
			}
		});
		
		cmd(HaCommand.EntryRemove, new MessageHandler() {
			@Override
			public void handle(Message msg, Session sess) throws IOException { 
				String serverAddr = msg.getServer();
				String entryId = msg.getMq();
				if(entryRemoveHandler != null){
					entryRemoveHandler.onEntryRemove(entryId, serverAddr);
				}
			} 
		});
		
		cmd(HaCommand.PubAll, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				if(msg.getBody() == null) return;
				ServerEntryTable table = ServerEntryTable.unpack(msg.getBodyString());
				if(pubAllHandler != null){
					pubAllHandler.onPubAll(table);
				}
			}
		});
	}
	
	public void cmd(String command, MessageHandler handler){
    	this.cmdHandlers.put(command, handler);
    }
	
	public void start(){
		for(MessageClient client : this.allTrackers){
			client.ensureConnectedAsync(); 
		}
	}
	
	public void sendSubAll(){
		for(MessageClient client : this.allTrackers){
			clientSubAll(client);
		}
	}
	
	private void clientSubAll(MessageClient client){
		try { 
    		Message msg = new Message(); 
    		msg.setCmd(HaCommand.SubAll); 
    		client.sendMessage(msg);
		} catch (IOException e) { 
			log.error(e.getMessage(), e);
		} catch (InterruptedException e) {
			//ignore
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
 
	public void onPubServerEntryList(PubAllHandler pubServerEntryListHandler){
		this.pubAllHandler = pubServerEntryListHandler;
	}

	public void onServerJoinHandler(ServerJoinHandler serverJoinHandler){
		this.serverJoinHandler = serverJoinHandler;
	}
	
	public void onServerLeaveHandler(ServerLeaveHandler serverLeaveHandler){
		this.serverLeaveHandler = serverLeaveHandler;
	}
	
	public void onEntryUpdateHandler(EntryUpdateHandler entryUpdateHandler){
		this.entryUpdateHandler = entryUpdateHandler;
	}

	public void onEntryRemoveHandler(EntryRemoveHandler entryRemoveHandler){
		this.entryRemoveHandler = entryRemoveHandler;
	}
	
	private PubAllHandler pubAllHandler;
	private ServerJoinHandler serverJoinHandler;
	private ServerLeaveHandler serverLeaveHandler;
	private EntryUpdateHandler entryUpdateHandler;
	private EntryRemoveHandler entryRemoveHandler;
    
	public static interface PubAllHandler{
		void onPubAll(ServerEntryTable table) throws IOException;
	}
	public static interface ServerJoinHandler{
		void onServerJoin(String serverAddr) throws IOException;
	}
	public static interface ServerLeaveHandler{
		void onServerLeave(String serverAddr) throws IOException;
	}
	public static interface EntryUpdateHandler{
		void onEntryUpdate(ServerEntry entry) throws IOException;
	}
	public static interface EntryRemoveHandler{
		void onEntryRemove(String entryId, String serverAddr)throws IOException;
	}
}
