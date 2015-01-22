package org.zbus.server;
 

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.zbus.common.Helper;
import org.zbus.common.MqInfo;
import org.zbus.common.Proto;
import org.zbus.common.TrackTable;
import org.zbus.common.json.JSONObject;
import org.zbus.common.json.parser.JSONParser;
import org.zbus.common.json.parser.ParseException;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.Message;
import org.zbus.remoting.MessageHandler;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.RemotingServer;
import org.zbus.remoting.ServerDispatcherManager;
import org.zbus.remoting.nio.Session;
import org.zbus.server.mq.info.BrokerInfo;
import org.zbus.server.mq.info.BrokerMqInfo;
 
 
public class TrackServer extends RemotingServer {  
	private static final Logger log = LoggerFactory.getLogger(TrackServer.class);
	private long brokerObsoleteTime = 10000; 
	private long publishInterval = 10000;
	private long probeInterval = 3000;
	private long lastPublishTime = 0;
	
	
	private Map<String,BrokerInfo> rawTrackMap = new HashMap<String,BrokerInfo>();
	private TrackTable trackTable = new TrackTable();
	private String fullTrackMapBuffer = null;
	
	private Map<String, Session> subscribers = new ConcurrentHashMap<String, Session>();
	private Map<String, RemotingClient> brokerProbes = new ConcurrentHashMap<String, RemotingClient>();
	
	protected final ScheduledExecutorService trackPubService = Executors.newSingleThreadScheduledExecutor();
	private ExecutorService trackExecutor = new ThreadPoolExecutor(4, 
			16, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	
	public TrackServer(int serverPort, ServerDispatcherManager dispatcherManager) { 
		this("0.0.0.0", serverPort, dispatcherManager);
	}
	
	public TrackServer(String serverHost, int serverPort,ServerDispatcherManager dispatcherManager) {
		super(serverHost, serverPort, dispatcherManager);
		this.serverName = "TrackServer";
		
		this.trackPubService.scheduleAtFixedRate(new Runnable() {	
			@Override
			public void run() {
				publishTrackTable();
			}
		}, 0, publishInterval, TimeUnit.MILLISECONDS);
		
		this.trackPubService.scheduleAtFixedRate(new Runnable() {	
			@Override
			public void run() {
				probeBrokers();
			}
		}, 0, probeInterval, TimeUnit.MILLISECONDS);
	}
	
	
	private void probeBrokers(){
		
		Iterator<Entry<String, RemotingClient>> iter = brokerProbes.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, RemotingClient> entry = iter.next();
			String brokerAddress = entry.getKey();
			RemotingClient client = entry.getValue();
			if(!client.hasConnected()){
				rawTrackMap.remove(brokerAddress);
				rebuildTrackTable(); 
				iter.remove();
			}
		}
		log.info("Track: "+ trackTable);
		
	}
	
	private void rebuildTrackTable(){
		this.trackTable = new TrackTable(); 
		Iterator<Entry<String, BrokerInfo>> iter = rawTrackMap.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, BrokerInfo> entry = iter.next();
			String broker = entry.getKey();
			BrokerInfo map = entry.getValue();
			if(map.isObsolete(brokerObsoleteTime)){
				iter.remove();
			}
			trackTable.addBroker(broker);
			
			//MQ路由表
			Map<String, List<MqInfo>> mqTable = trackTable.getMqTable();
			
			Map<String, BrokerMqInfo> rawMqTable = map.getMqTable();
			for(String mqName : rawMqTable.keySet()){
				BrokerMqInfo raw = rawMqTable.get(mqName);
				MqInfo info = raw.generateMqInfo();
				info.setBroker(broker);
				
				List<MqInfo> infoList = mqTable.get(mqName);
				if(infoList == null){
					infoList = new ArrayList<MqInfo>();
					mqTable.put(mqName, infoList);
				}
				
				infoList.add(info);
			}  
		}
	}
	 
	private void publishTrackTable(){
		String curFullTrackMapBuffer = trackTable.toString();
		//log.info("Track: " + curFullTrackMapBuffer);
		if(subscribers.size()<1) return;  
		
		if(curFullTrackMapBuffer.equals(fullTrackMapBuffer)){
			if(System.currentTimeMillis()-lastPublishTime<publishInterval){
				return;
			}
		}
		lastPublishTime = System.currentTimeMillis();
		fullTrackMapBuffer = curFullTrackMapBuffer;
		Message msg = new Message();
		msg.setCommand(Proto.TrackPub);
		msg.setBody(fullTrackMapBuffer);
		
		Iterator<Entry<String, Session>> iter = subscribers.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, Session> entry = iter.next();
			Session sess = entry.getValue();
			if(!sess.isActive()){
				iter.remove();
				continue;
			}
			try {
				sess.write(msg);
			} catch (IOException e) {  
				iter.remove();
				//ignore
			}
		}
		
	}
	
	
	@Override
	public void init() { 
		this.registerHandler(Proto.Heartbeat, new MessageHandler() {
			
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {
				//ignore;
			}
		});
		
		this.registerHandler(Proto.TrackReport, new MessageHandler() {  
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {  
				JSONParser parser = new JSONParser();
				JSONObject json = null;
				try {
					json = (JSONObject) parser.parse(msg.getBodyString());
				} catch (ParseException e) {
					log.error(e.getMessage(), e);
					return;
				}
				final BrokerInfo map = BrokerInfo.fromJson(json);
				final String brokerAddress = map.getBroker();
				//log.info("Broker Report:\n%s", map); 
				if(!brokerProbes.containsKey(brokerAddress)){
					final RemotingClient client = new RemotingClient(brokerAddress);
					trackExecutor.submit(new Runnable() {
						@Override
						public void run() { 
							try {
								client.connectIfNeed();
								brokerProbes.put(brokerAddress, client);
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
						}
					});
				}
				
				rawTrackMap.put(brokerAddress, map);
				
				rebuildTrackTable();  
				publishTrackTable(); 
			}
		});
		
		this.registerHandler(Proto.TrackSub, new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {
				subscribers.put(sess.id(), sess);
				if(fullTrackMapBuffer == null){
					rebuildTrackTable();
					fullTrackMapBuffer = trackTable.toString();
				}
				msg.setStatus("200");
				msg.setBody(fullTrackMapBuffer);
				sess.write(msg);
			}
		}); 
	}     
	
	
	
	public static void main(String[] args) throws Exception{
		int serverPort = Helper.option(args, "-p", 16666);
		ServerDispatcherManager dispachterManager = new ServerDispatcherManager();
		TrackServer track = new TrackServer(serverPort, dispachterManager); 
		track.start(); 
	} 
	
}
