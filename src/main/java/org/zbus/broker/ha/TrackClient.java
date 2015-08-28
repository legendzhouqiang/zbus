package org.zbus.broker.ha;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.zbus.broker.ha.BrokerEntry.BrokerEntryPrioritySet;
import org.zbus.log.Logger;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

import com.alibaba.fastjson.JSON;

public class TrackClient extends MessageClient {  
	
	public TrackClient(String address, Dispatcher dispatcher) {
		super(address, dispatcher); 
	} 
	public TrackClient(String host, int port, Dispatcher dispatcher) {
		super(host, port, dispatcher); 
	} 
	
	public void subEntryUpdate(){
		
	}
	
	public void pubEntryUpdate(BrokerEntry be) throws IOException{
		Message msg = new Message();
		msg.setBody(JSON.toJSONString(be));
		msg.setCmd(Protocol.EntryPub);
		invokeAsync(msg, null); 
	}
	
	public Map<String, BrokerEntryPrioritySet> queryAllBrokerEntries(){
		return null;
	}  
	
	
	public static class TrackClientSet implements Closeable{
		private static final Logger log = Logger.getLogger(TrackClientSet.class);
		private Set<TrackClient> allClients = new HashSet<TrackClient>();
		private Set<TrackClient> healthyClients = new HashSet<TrackClient>();
		private int reconnectTimeout = 3000; //ms
	
		public TrackClientSet(Dispatcher dispatcher, String trackServerList){  
	    	String[] blocks = trackServerList.split("[;]");
	    	for(String block : blocks){
	    		final String address = block.trim();
	    		if(address.equals("")) continue;
	    		
	    		final TrackClient trackClient = new TrackClient(address, dispatcher); 
	    		allClients.add(trackClient);
	    		trackClient.onError(new ErrorHandler() {
	    			@Override
	    			public void onError(IOException e, Session sess)
	    					throws IOException { 
	    				healthyClients.remove(trackClient);
	    				log.info("TrackServer(%s) down, try to reconnect in %d seconds", address, reconnectTimeout/1000);
	    				try { Thread.sleep(reconnectTimeout); } catch (InterruptedException ex) { }
	    				trackClient.connectAsync();
	    			}  
				});
	    		
	    		trackClient.onConnected(new ConnectedHandler() {
	    			@Override
	    			public void onConnected(Session sess) throws IOException {  
	    				log.info("TrackServer(%s) connected", address);
	    				healthyClients.add(trackClient);
	    			}
				});
	    		
	    		try {
					trackClient.connectAsync();
				} catch (IOException e) { 
					e.printStackTrace();
				}
	    	} 
	    }
	    
	    public void pubEntryUpdate(BrokerEntry be){   
	    	for(TrackClient trackClient : this.healthyClients){
		    	try { 
					trackClient.pubEntryUpdate(be);
				} catch (IOException e) { 
					//log.error(e.getMessage(), e);
				}
	    	}
	    }
	    
	    @Override
	    public void close() throws IOException { 
	    	for(TrackClient client : allClients){
	    		client.close();
	    	}
	    	allClients.clear();
	    	healthyClients.clear();
	    }
	}
}
