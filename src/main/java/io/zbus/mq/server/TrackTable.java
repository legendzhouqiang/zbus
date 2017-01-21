package io.zbus.mq.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Message;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.util.JsonUtil;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;

public class TrackTable {
	private static final Logger log = LoggerFactory.getLogger(TrackTable.class);
	
	private Map<String, ServerInfo> serverMap = new ConcurrentHashMap<String, ServerInfo>();
	private final MqAdaptor mqAdaptor;
	private final String thisServerAddress;
	
	public TrackTable(MqAdaptor mqAdaptor, String serverAddress){
		this.mqAdaptor = mqAdaptor;
		thisServerAddress = serverAddress;
	} 
	
	public Map<String, ServerInfo> buildServerTable(){
		Map<String, ServerInfo> map = new HashMap<String, ServerInfo>(serverMap);
		map.put(thisServerAddress, mqAdaptor.getServerInfo());
		return map;
	} 
	
	public void update(ServerInfo serverInfo){
		log.info("Update server: " + JsonUtil.toJSONString(serverInfo));
		serverMap.put(serverInfo.serverAddress, serverInfo);
	}
	
	public void update(TopicInfo topicInfo){
		log.info("Update topic: " + JsonUtil.toJSONString(topicInfo));
		
		ServerInfo serverInfo = serverMap.get(topicInfo.serverAddress);
		String serverAddress = topicInfo.serverAddress;
		if(serverInfo == null){
			serverInfo = new ServerInfo();
			serverInfo.serverAddress = serverAddress;
			serverInfo.topicMap.put(topicInfo.topicName, topicInfo);
			serverMap.put(serverAddress, serverInfo);
		}
	}
	
	public void update(Message message){
		String type = message.getHeader(Protocol.TRACK_TYPE); 
		
		if(Protocol.TRACK_SERVER.equalsIgnoreCase(type)){
			ServerInfo serverInfo = JsonUtil.parseObject(message.getBodyString(), ServerInfo.class);
			update(serverInfo);
			return;
		}
		//default to TopicInfo
		TopicInfo topicInfo = JsonUtil.parseObject(message.getBodyString(), TopicInfo.class);
		update(topicInfo);  
	}
}
