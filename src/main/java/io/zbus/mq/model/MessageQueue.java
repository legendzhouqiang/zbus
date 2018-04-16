package io.zbus.mq.model;

import java.util.List;
import java.util.Map;

/** 
 *  
 * MessageQueue:
 * 
 *      queue_name |||||||||||(message/topic)|||||||||||||||
 *                 ------- channel1
 *                 ------- channel2
 *                 
 * By default: 
 *   channel = unique generated, each subscriber with unique channel
 *   
 * Noted: Topic is an attribute of a message, not a message queue. A Message Queue may contains messages with different topics
 * 
 * Queue   -- message container, with name as identifier
 * Channel -- subscriber isolation, each channel share same message pointer for reading
 * Topic   -- message's topic, subscriber may filter on it. e.g. /abc, follows MQTT standard
 * 
 * Flexible messaging models based on Channel
 * 
 * 1) PubSub: default, each subscriber generated unique channel
 * 2) LoadBalance: subscribers share same channel
 * 3) Mixed: each group of subscribers share a same channel
 *  
 * MQTT compatible, with queue default to null(empty) 
 * 
 * 
 * @author Hong Leiming
 *
 */
public interface MessageQueue { 
	String name();
	
	void write(Object... message); 
	List<Object> read(String channelId, int count);  
	
	Channel channel(String channelId);
	void saveChannel(Channel channel);
	void removeChannel(String channelId);  
	
	Map<String, Object> attributes();
	void flush();
}
