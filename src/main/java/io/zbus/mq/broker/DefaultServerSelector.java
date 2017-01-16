package io.zbus.mq.broker;

import java.util.List;
import java.util.Random;

import io.zbus.mq.Broker.ServerSelector;
import io.zbus.mq.Broker.ServerTable;
import io.zbus.mq.Broker.TopicInfo;

public class DefaultServerSelector implements ServerSelector {
	private Random random = new Random();

	@Override
	public String selectForProducer(ServerTable table, String topic) {
		int serverCount = table.activeList.size();
		if (serverCount == 0) {
			return null;
		}
		List<TopicInfo> topicList = table.topicMap.get(topic);
		if (topicList == null || topicList.size() == 0) {
			return table.activeList.get(random.nextInt(serverCount));
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
	public String selectForConsumer(ServerTable table, String topic) {
		int serverCount = table.activeList.size();
		if (serverCount == 0) {
			return null;
		}
		List<TopicInfo> topicInfoList = table.topicMap.get(topic);
		if (topicInfoList == null || topicInfoList.size() == 0) {
			return table.activeList.get(random.nextInt(serverCount));
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