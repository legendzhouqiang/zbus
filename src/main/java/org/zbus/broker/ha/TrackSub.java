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
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageClient;

public class TrackSub implements Closeable{  
	private static final Logger log = Logger.getLogger(TrackSub.class);
	private Set<MessageClient> allClients = new HashSet<MessageClient>();
	private Set<MessageClient> healthyClients = new HashSet<MessageClient>();
	private int reconnectTimeout = 3000; //ms
	
	public TrackSub(String trackServerList, Dispatcher dispatcher) {
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
    				
    				clientSubscribe(client);
    			}
			}); 
    	} 
    }
	
	public void start(){
		for(MessageClient client : this.allClients){
			try {
				client.connectAsync();
			} catch (IOException e) { 
				log.error(e.getMessage(), e);
			}
		}
	}
	
	private void clientSubscribe(MessageClient client){
		try { 
    		Message msg = new Message(); 
    		msg.setCmd(HaCommand.Subscribe);
    		client.send(msg);
		} catch (IOException e) { 
			//log.error(e.getMessage(), e);
		}
	} 
    
    public void onMessage(MessageHandler msgHandler){
    	for(MessageClient client : this.allClients){
    		client.onMessage(msgHandler);
    	}
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
