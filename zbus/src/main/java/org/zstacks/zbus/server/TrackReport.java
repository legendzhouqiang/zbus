package org.zstacks.zbus.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.zstacks.zbus.protocol.BrokerInfo;
import org.zstacks.zbus.protocol.MqInfo;
import org.zstacks.zbus.protocol.Proto;
import org.zstacks.zbus.server.mq.MessageQueue;
import org.zstacks.znet.Message;
import org.zstacks.znet.RemotingClient;
import org.zstacks.znet.callback.ErrorCallback;
import org.zstacks.znet.nio.Dispatcher;
import org.zstacks.znet.nio.Session;

import com.alibaba.fastjson.JSON;

public class TrackReport implements Closeable {
	private long trackDelay = 1000;
	private long trackInterval = 3000;
	
	private final String zbusServerAddr;
	private final ConcurrentMap<String, MessageQueue> mqTable; 
	private final List<RemotingClient> clients = new ArrayList<RemotingClient>();
	private final ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
	private ExecutorService reportService = new ThreadPoolExecutor(4,16, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	
	
	public TrackReport(ConcurrentMap<String, MessageQueue> mqTable, String zbusServerAddr){
		this.mqTable = mqTable; 
		this.zbusServerAddr = zbusServerAddr; 
	}
	 
	
	public void startTrackReport(String trackServerAddr, Dispatcher dispatcher) throws IOException{ 
		if(trackServerAddr == null) return;
		 
		String[] serverAddrs = trackServerAddr.split("[;]");
		
		for(String addr : serverAddrs){
			addr = addr.trim();
			if( addr.length() == 0 ) continue;
			
			RemotingClient client = new RemotingClient(addr, dispatcher);
			client.setErrorCallback(new ErrorCallback() {  
				public void onError(IOException e, Session sess) throws IOException { }
			});
			clients.add(client);
		} 
		
		
		this.scheduledService.scheduleAtFixedRate(new Runnable() {
			public void run() { 
				reportToTrackServer();
			}
		}, trackDelay, trackInterval, TimeUnit.MILLISECONDS);
	} 
	
	public Message packServerInfo(){
		Map<String, MqInfo> table = new HashMap<String, MqInfo>();
   		for(Map.Entry<String, MessageQueue> e : this.mqTable.entrySet()){
   			table.put(e.getKey(), e.getValue().getMqInfo());
   		} 
		Message msg = new Message(); 
		BrokerInfo info = new BrokerInfo();
		info.setBroker(zbusServerAddr);
		info.setMqTable(table);  
		msg.setBody(JSON.toJSONString(info));
		return msg;
	}
	
	public void reportToTrackServer(){
		reportService.submit(new Runnable() { 
			public void run() {
				Message msg = packServerInfo();
				msg.setCommand(Proto.TrackReport);
				for(RemotingClient client : clients){
					try { client.invokeAsync(msg, null); } catch (IOException e) { }//ignore 
				} 
			}
		}); 		
	}
	
	
	public void close() throws IOException{
		for(RemotingClient client : this.clients){
			client.close();
		}   
		this.scheduledService.shutdown();
		this.reportService.shutdown(); 
	}

	public long getTrackDelay() {
		return trackDelay;
	}

	public void setTrackDelay(long trackDelay) {
		this.trackDelay = trackDelay;
	}

	public long getTrackInterval() {
		return trackInterval;
	}

	public void setTrackInterval(long trackInterval) {
		this.trackInterval = trackInterval;
	}
}
