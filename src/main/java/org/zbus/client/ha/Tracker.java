package org.zbus.client.ha;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.logging.Logger;
import org.logging.LoggerFactory;
import org.remoting.ClientDispachterManager;
import org.remoting.Message;
import org.remoting.RemotingClient;
import org.remoting.callback.ErrorCallback;
import org.remoting.callback.MessageCallback;
import org.remoting.ticket.ResultCallback;
import org.zbus.common.Proto;
import org.zbus.common.TrackTable;
import org.znet.Session;

import com.alibaba.fastjson.JSON;

interface TrackListener{
	void onTrackTableUpdated(TrackTable trackTable);
}

class Tracker {
	static final Logger log = LoggerFactory.getLogger(Tracker.class);
	
	String trackServerList="127.0.0.1:16666"; 
	final List<RemotingClient> clients = new ArrayList<RemotingClient>();
	ClientDispachterManager clientMgr;  
	CountDownLatch tableReady = new CountDownLatch(1);
	ExecutorService executor = new ThreadPoolExecutor(4, 16, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	List<TrackListener> trackListeners = new ArrayList<TrackListener>();
	
	
	public Tracker(String trackServerList) throws IOException{
		this(trackServerList, null);
	} 
	
	public Tracker(String trackServerList, ClientDispachterManager clientMgr) throws IOException {  	
		this.clientMgr = clientMgr;  
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
			if( addr.isEmpty() ) continue;
			
			final RemotingClient client = new RemotingClient(addr, this.clientMgr); 
			clients.add(client);
			
			executor.submit(new Runnable() { 
				@Override
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
		client.onMessage(new MessageCallback() { 
			@Override
			public void onMessage(Message msg, Session sess) throws IOException { 
				final TrackTable trackTable = JSON.parseObject(msg.getBody(), TrackTable.class);
				for(TrackListener listener : trackListeners){
					listener.onTrackTableUpdated(trackTable);
				}
				tableReady.countDown();
			}
		});
		
		client.onError(new ErrorCallback() { 
			@Override
			public void onError(IOException e, Session sess) throws IOException {
				executor.submit(new Runnable() { 
					@Override
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
				@Override
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
	
}
