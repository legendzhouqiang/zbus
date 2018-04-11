package io.zbus.mq.model;

import java.util.List;

/**
 * 
 * Domain of a MessageQueue -- identifies a message queue. 
 * Noted: Topic is an attribute of a message, not for a message queue. A Message Queue may contains messages with different topics
 * 
 * 
 * MQTT compatible, with domain default to null(empty)
 * 
 *      domain |||||||||||(message/topic)|||||||||||||||
 *                 ------- channel1
 *                 ------- channel2
 *                 
 * By default:
 *   domain = null, 
 *   channel = unique generated, each subscriber with unique channel
 * 
 * Domain  -- message queue identifier, every message with topic resides in a domain
 * Channel -- subscriber isolation, each channel share same message pointer for reading
 * Topic   -- message's topic, subscriber may filter on it. e.g. /abc, follows MQTT standard
 * 
 * Flexible messaging models based on Channel
 * 
 * 1) PubSub: default, each subscriber generated unique channel
 * 2) LoadBalance: subscribers share same channel
 * 3) Mixed: each group of subscribers share a same channel
 * 
 * @author Hong Leiming
 *
 */
public interface MessageQueue { 
	Domain getDomain(); 
	void setDomain(Domain domain); 
	
	Channel getChannel(String channelId);
	void addChannel(Channel channel);
	void removeChannel(String channelId);
	void updateChannel(Channel channel);
	
	void write(Object message);
	Object read(String channelId); 
	List<Object> read(String channelId, int batchSize); 
}
