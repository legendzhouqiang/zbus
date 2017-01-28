package io.zbus.mq.broker;

import java.util.List;

import io.zbus.mq.Broker.RouteTable;
import io.zbus.mq.Broker.ServerSelector;
import io.zbus.mq.Protocol.TopicInfo;

public class DefaultServerSelector implements ServerSelector { 
	@Override
	public String selectForProducer(RouteTable table, String topic) {
		int serverCount = table.serverMap().size();
		if (serverCount == 0) {
			return null;
		}
		List<TopicInfo> topicList = table.topicTable().get(topic);
		if (topicList == null || topicList.size() == 0) {
			return table.randomServerInfo().serverAddress;
		}
		TopicInfo target = topicList.get(0);
		for (int i = 1; i < topicList.size(); i++) {
			TopicInfo current = topicList.get(i);
			if (target.consumerCount < current.consumerCount) { //consumer count decides
				target = current;
			}
		}
		return target.serverAddress;
	}

	@Override
	public String selectForConsumer(RouteTable table, String topic) {
		int serverCount = table.serverMap().size();
		if (serverCount == 0) {
			return null;
		}
		List<TopicInfo> topicInfoList = table.topicTable().get(topic);
		if (topicInfoList == null || topicInfoList.size() == 0) {
			return table.randomServerInfo().serverAddress;
		}
		TopicInfo target = topicInfoList.get(0);
		for (int i = 1; i < topicInfoList.size(); i++) {
			TopicInfo current = topicInfoList.get(i);
			if (target.messageCount < current.messageCount) { // message count decides
				target = current;
			}
		}
		return target.serverAddress;
	}
}