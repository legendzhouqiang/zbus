package org.zbus.server;
 

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.common.BrokerInfo;
import org.zbus.common.BrokerMqInfo;
import org.zbus.common.MqInfo;
import org.zbus.common.Proto;
import org.zbus.common.TrackTable;
import org.zbus.json.JSONObject;
import org.zbus.json.parser.JSONParser;
import org.zbus.json.parser.ParseException;
import org.zbus.logging.Logger;
import org.zbus.logging.LoggerFactory;
import org.zbus.remoting.Helper;
import org.zbus.remoting.Message;
import org.zbus.remoting.MessageHandler;
import org.zbus.remoting.RemotingServer;
import org.zbus.remoting.znet.Session;
 
 
public class TrackServer extends RemotingServer {  
	private static final Logger log = LoggerFactory.getLogger(TrackServer.class);
	private long brokerObsoleteTime = 10000; 
	private long publishInterval = 10000;
	private long lastPublishTime = 0;
	
	
	private Map<String,BrokerInfo> rawTrackMap = new HashMap<String,BrokerInfo>();
	private TrackTable trackTable = new TrackTable();
	private String fullTrackMapBuffer = null;
	
	private Map<String, Session> subscribers = new ConcurrentHashMap<String, Session>();
	
	public TrackServer(int serverPort) {
		super(serverPort);
		this.serverName = "TrackServer";
	}
	public TrackServer(String serverHost, int serverPort) {
		super(serverHost, serverPort);
		this.serverName = "TrackServer";
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
		if(subscribers.size()<1) return; 
		//String curFullTrackMapBuffer = JSON.toJSONString(trackTable, true);
		String curFullTrackMapBuffer = trackTable.toString();
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
				BrokerInfo map = BrokerInfo.fromJson(json);
				log.info("Broker Report:\n%s", map);
				
				rawTrackMap.put(map.getBroker(), map);
				
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
		TrackServer track = new TrackServer(serverPort); 
		track.start(); 
	} 
	
}
