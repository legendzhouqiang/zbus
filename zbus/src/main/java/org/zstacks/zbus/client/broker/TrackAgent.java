package org.zstacks.zbus.client.broker;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zstacks.zbus.protocol.Proto;
import org.zstacks.zbus.protocol.TrackTable;
import org.zstacks.znet.Message;
import org.zstacks.znet.RemotingClient;
import org.zstacks.znet.callback.ErrorCallback;
import org.zstacks.znet.callback.MessageCallback;
import org.zstacks.znet.nio.Dispatcher;
import org.zstacks.znet.nio.Session;
import org.zstacks.znet.ticket.ResultCallback;

import com.alibaba.fastjson.JSON;

interface TrackListener{
	void onTrackTableUpdated(TrackTable trackTable);
}

public class TrackAgent implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(TrackAgent.class);
	private String trackServerList="127.0.0.1:16666"; 
	private final List<RemotingClient> clients = new ArrayList<RemotingClient>();
	private Dispatcher dispatcher;  
	private CountDownLatch tableReady = new CountDownLatch(1);
	private List<TrackListener> trackListeners = new ArrayList<TrackListener>();
	

	public TrackAgent(String trackServerList, Dispatcher dispatcher) throws IOException {  	
		this.dispatcher = dispatcher;  
		this.connectToTrackServers();
		
	} 
	
	public void waitForReady(long timeout){
		try {
			this.tableReady.await(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {  
		} 
	}
	
	public void addTrackListener(TrackListener listener){
		this.trackListeners.add(listener);
	}
	
	public void removeTrackListener(TrackListener listener){
		this.trackListeners.remove(listener);
	}
	
	
	private void connectToTrackServers(){
		String[] serverAddrs = this.trackServerList.split("[;]");
		for(String addr : serverAddrs){
			addr = addr.trim();
			if( addr.length() == 0 ) continue;
			
			final RemotingClient client = new RemotingClient(addr, this.dispatcher); 
			clients.add(client);
			
			dispatcher.asyncRun(new Runnable() { 
				public void run() { 
					try {
						initTrackClient(client);
					} catch (IOException e) {  
						//log.error(e.getMessage(), e);
					} 
				}
			});
			
		}  
	}
	
	
	private void initTrackClient(final RemotingClient client) throws IOException{  
		client.setMessageCallback(new MessageCallback() { 
			public void onMessage(Message msg, Session sess) throws IOException { 
				final TrackTable trackTable = JSON.parseObject(msg.getBody(), TrackTable.class);
				for(TrackListener listener : trackListeners){
					listener.onTrackTableUpdated(trackTable);
				}
				tableReady.countDown();
			}
		});
		
		client.setErrorCallback(new ErrorCallback() { 
			public void onError(IOException e, Session sess) throws IOException {
				dispatcher.asyncRun(new Runnable() { 
					public void run() { 
						doTrackSub(client);
					}
				});
				
			}
		}); 
		doTrackSub(client); 
		 
	}
	
	private void doTrackSub(final RemotingClient client){
		try {  
			Message msg = new Message();
			msg.setCommand(Proto.TrackSub); 
			client.invokeAsync(msg, new ResultCallback() { 
				public void onCompleted(Message result) {
					final TrackTable trackTable = JSON.parseObject(result.getBody(), TrackTable.class);
					for(TrackListener listener : trackListeners){
						listener.onTrackTableUpdated(trackTable);
					}
					tableReady.countDown();
				}  
			}); 
		} catch (IOException e) { 
			log.debug(e.getMessage(), e);;
		}
	}

	@Override
	public void close() throws IOException { 
		for(RemotingClient client : this.clients){
			client.close();
		}
	}
}
