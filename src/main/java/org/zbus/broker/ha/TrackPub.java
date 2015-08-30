package org.zbus.broker.ha;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.zbus.log.Logger;
import org.zbus.net.Client.ConnectedHandler;
import org.zbus.net.Client.ErrorHandler;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class TrackPub implements Closeable{  
	private static final Logger log = Logger.getLogger(TrackPub.class);
	private Set<MessageClient> allTrackers = new HashSet<MessageClient>();
	private Set<MessageClient> healthyTrackers = new HashSet<MessageClient>();
	private int reconnectTimeout = 3000; //ms
	private ConnectedHandler connectedHandler;
	
	public TrackPub(String trackServerList, Dispatcher dispatcher) {
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
    				log.info("TrackServer(%s) down, try to reconnect in %d seconds", address, reconnectTimeout/1000);
    				try { Thread.sleep(reconnectTimeout); } catch (InterruptedException ex) { }
    				client.connectAsync();
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected(Session sess) throws IOException {  
    				log.info("TrackServer(%s) connected", address);
    				healthyTrackers.add(client); 
    				if(connectedHandler != null){
    					connectedHandler.onConnected(sess);
    				}
    			}
			}); 
    	} 
    }
	
	public void start(){
		for(MessageClient client : allTrackers){
			try {
				client.connectAsync();
			} catch (IOException e) { 
				log.error(e.getMessage(), e);
			}
		}
	}
	
	public void sendToAllTrackers(Message msg){
		for(MessageClient client : healthyTrackers){ 
			try{
				msg.removeHead(Message.ID);
				client.send(msg);
			}catch(IOException e){
				log.error(e.getMessage(), e);
			}
    	}
	}
	
	public void pubTargetJoin(String targetServerAddr){
		Message msg = new Message();
		msg.setCmd(HaCommand.ServerJoin);
		msg.setServer(targetServerAddr); 
		sendToAllTrackers(msg);
	}
	
	public void pubTargetLeave(String targetServerAddr){
		Message msg = new Message();
		msg.setCmd(HaCommand.ServerLeave);
		msg.setServer(targetServerAddr);
		sendToAllTrackers(msg);
	}
	
	public void pubEntryUpdate(ServerEntry be){ 
		Message msg = new Message();
		msg.setBody(be.toJsonString());
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
