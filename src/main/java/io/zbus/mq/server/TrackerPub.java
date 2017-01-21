
package io.zbus.mq.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.zbus.mq.Message;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.net.EventDriver;
import io.zbus.util.JsonUtil;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;

/**
 * 
 * Publish current MqServer status to other MqServers(Tracker)
 * 
 * @author Rushmore
 *
 */
public class TrackerPub implements Closeable{  
	private static final Logger log = LoggerFactory.getLogger(TrackerPub.class);
	
	private Set<MessageClient> allTrackers = new HashSet<MessageClient>();
	private Set<MessageClient> healthyTrackers = new HashSet<MessageClient>();
	private ConnectedHandler connectedHandler;
	private EventDriver eventDriver;
	private final String trackServerList;
	
	public TrackerPub(String trackServerList, EventDriver driver) {  
		this.trackServerList = trackServerList;
		this.eventDriver = driver; 
    }
	
	public void start(){
		if(trackServerList == null || trackServerList.isEmpty()) return;
		
		log.info("Start tracking to " + this.trackServerList);
		
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
    				log.info("Tracker(%s) connected", address);
    				healthyTrackers.add(client); 
    				if(connectedHandler != null){
    					connectedHandler.onConnected();
    				}
    			}
			}); 
    	} 
    	
		for(final MessageClient client : allTrackers){
			client.ensureConnectedAsync();
		}
	}
	
	public void reportUpdate(ServerInfo serverInfo){
		if(healthyTrackers.isEmpty()) return;
		
		Message message = new Message();
		message.setCommand(Protocol.TRACK_PUB);
		message.setHeader(Protocol.TRACK_TYPE, Protocol.TRACK_SERVER);
		message.setBody(JsonUtil.toJSONString(serverInfo));
		
		report(message);
	}
	
	public void reportUpdate(TopicInfo topicInfo){
		if(healthyTrackers.isEmpty()) return;
		
		Message message = new Message();
		message.setCommand(Protocol.TRACK_PUB);
		message.setHeader(Protocol.TRACK_TYPE, Protocol.TRACK_TOPIC);
		message.setBody(JsonUtil.toJSONString(topicInfo));
		
		report(message);
	}
	
	private void report(final Message message){ 
		if(healthyTrackers.isEmpty()) return;
		
		//run in thread to prevent potential blocking
		eventDriver.getGroup().submit(new Runnable() { 
			@Override
			public void run() {
				for(MessageClient client : healthyTrackers){ 
					try{ 
						message.removeHeader(Protocol.ID); //send multiple times
						if(client.hasConnected()){
							client.sendMessage(message);
						}
					} catch (IOException e){
						log.error(e.getMessage(), e);
					} catch (InterruptedException e) {
						break;
					}
		    	}
			}
		}); 
	} 
	 
	public void onTrackerConnected(ConnectedHandler connectedHandler){
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
