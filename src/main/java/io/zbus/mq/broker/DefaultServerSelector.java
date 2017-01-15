package io.zbus.mq.broker;

import java.util.List;
import java.util.Random;

import io.zbus.mq.Broker.ServerSelector;
import io.zbus.mq.Broker.ServerTable;
import io.zbus.mq.Broker.TopicInfo;

public class DefaultServerSelector implements ServerSelector{
	private Random random = new Random();
	
	@Override
	public String selectForProducer(ServerTable table, String topic) { 
		int serverCount = table.activeServerList.size();
		if(serverCount == 0){
			return null;
		} 
		List<TopicInfo> topicInfoList = table.topicMap.get(topic);
		if(topicInfoList == null || topicInfoList.size() == 0){
			return table.activeServerList.get(random.nextInt(serverCount));
		}
		TopicInfo target = topicInfoList.get(0);
		for(int i=1; i<topicInfoList.size(); i++){
			TopicInfo current = topicInfoList.get(i);
			 if(target.consumerCount < current.consumerCount){
				 target = current;
			 }
		}
		return target.serverAddress;
	}

	@Override
	public String selectForConsumer(ServerTable table, String topic) { 
		int serverCount = table.activeServerList.size();
		if(serverCount == 0){
			return null;
		} 
		List<TopicInfo> topicInfoList = table.topicMap.get(topic);
		if(topicInfoList == null || topicInfoList.size() == 0){
			return table.activeServerList.get(random.nextInt(serverCount));
		}
		TopicInfo target = topicInfoList.get(0);
		for(int i=1; i<topicInfoList.size(); i++){
			TopicInfo current = topicInfoList.get(i);
			 if(target.messageCount < current.messageCount){
				 target = current;
			 }  
		}
		return target.serverAddress;
	}
}