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
 * 
 * @author leiming.hong
 *
 */
public interface MessageQueue { 
	/**
	 * Name of message queue, identifier
	 * @return
	 */
	String name();
	/**
	 * Write message to queue, support batch insert
	 * 
	 * @param message message or message list
	 */
	void write(Object... message); 
	/**
	 * Read message from queue by channel, support batch read
	 * If the length of result is less than count, queue end reached.
	 * 
	 * @param channelId id of channel
	 * @param count maximum count of message to read
	 * @return
	 */
	List<Object> read(String channelId, int count);  
	
	/**
	 * Get channel by id
	 * 
	 * @param channelId id of channel
	 * @return Channel object
	 */
	Channel channel(String channelId);
	
	/**
	 * Add or update channel to the queue
	 * @param channel Channel object to save
	 */
	void addChannel(Channel channel);
	
	/**
	 * Remove channel by channel's Id
	 * 
	 * @param channelId
	 */
	void removeChannel(String channelId); 
	
	/** 
	 * @return all channels inside of the queue
	 */
	Map<String, Channel> channels(); 
	
	/** 
	 * @return attribute map of the queue
	 */
	Long mask();
	
	/**
	 * Flush message in memory to disk if support
	 */
	void flush();
	
	/**
	 * Destroy of this queue
	 */
	void destroy();
}
