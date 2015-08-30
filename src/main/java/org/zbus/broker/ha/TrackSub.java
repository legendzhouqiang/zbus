package org.zbus.broker.ha;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
	
	private Set<MessageClient> allTrackers = new HashSet<MessageClient>();
	private Set<MessageClient> healthyTrackers = new HashSet<MessageClient>();
	private int reconnectTimeout = 3000; //ms
	
	private Map<String, MessageHandler> cmdHandlers = new ConcurrentHashMap<String, MessageHandler>();
	
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
    				
    				clientSubscribe(client);
    			}
			}); 
    		
    		client.onMessage(new MessageHandler() { 
				@Override
				public void handle(Message msg, Session sess) throws IOException { 
					log.info("%s", msg);
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
	
	private void clientSubscribe(MessageClient client){
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
}
