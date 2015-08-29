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
	private Set<MessageClient> allClients = new HashSet<MessageClient>();
	private Set<MessageClient> healthyClients = new HashSet<MessageClient>();
	private int reconnectTimeout = 3000; //ms
	private ConnectedHandler connectedHandler;
	
	public TrackPub(String trackServerList, Dispatcher dispatcher) {
		String[] blocks = trackServerList.split("[;]");
    	for(String block : blocks){
    		final String address = block.trim();
    		if(address.equals("")) continue;
    		
    		final MessageClient client = new MessageClient(address, dispatcher); 
    		allClients.add(client);
    		
    		client.onError(new ErrorHandler() {
    			@Override
    			public void onError(IOException e, Session sess)
    					throws IOException { 
    				healthyClients.remove(client);
    				log.info("TrackServer(%s) down, try to reconnect in %d seconds", address, reconnectTimeout/1000);
    				try { Thread.sleep(reconnectTimeout); } catch (InterruptedException ex) { }
    				client.connectAsync();
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected(Session sess) throws IOException {  
    				log.info("TrackServer(%s) connected", address);
    				healthyClients.add(client); 
    				if(connectedHandler != null){
    					connectedHandler.onConnected(sess);
    				}
    			}
			}); 
    	} 
    }
	
	public void start(){
		for(MessageClient client : allClients){
			try {
				client.connectAsync();
			} catch (IOException e) { 
				log.error(e.getMessage(), e);
			}
		}
	}
	
	private void sendToAll(Message msg){
		for(MessageClient client : healthyClients){ 
			try{
				msg.removeHead(Message.ID);
				client.send(msg);
			}catch(IOException e){
				log.error(e.getMessage(), e);
			}
    	}
	}
	
	public void pubBrokerJoin(String broker){
		Message msg = new Message();
		msg.setCmd(HaCommand.BrokerJoin);
		msg.setBroker(broker);
		sendToAll(msg);
	}
	
	public void pubBrokerLeave(String broker){
		Message msg = new Message();
		msg.setCmd(HaCommand.BrokerLeave);
		msg.setBroker(broker);
		sendToAll(msg);
	}
	
	public void pubEntryUpdate(BrokerEntry be){ 
		Message msg = new Message();
		msg.setBody(be.toJsonString());
		msg.setCmd(HaCommand.EntryUpdate);  
		sendToAll(msg);
	} 
	
	public void pubEntryRemove(String entryId){ 
		Message msg = new Message();
		msg.setBody(entryId);
		msg.setCmd(HaCommand.EntryRemove);  
		sendToAll(msg);
	} 
	
	public void onConnected(ConnectedHandler connectedHandler){
		this.connectedHandler = connectedHandler;
	}
	
    @Override
    public void close() throws IOException { 
    	for(MessageClient client : allClients){
    		client.close();
    	}
    	allClients.clear();
    	healthyClients.clear();
    }
}
