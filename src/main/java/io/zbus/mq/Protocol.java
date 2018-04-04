package io.zbus.mq;

/**
 * 
 * MQTT compatible, with domain default to null(empty)
 * 
 *      domain |||||||||||(/topic)|||||||||||||||
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
public interface Protocol {
	public static final String CMD    = "cmd";
	public static final String PUB    = "pub";
	public static final String SUB    = "sub"; 
	public static final String PING   = "ping";
	
	public static final String TOPIC  = "topic"; 
	public static final String DOMAIN = "domain";
	public static final String CHANNEL= "channel"; 
	
	public static final String DATA   = "data";  
	public static final String ID     = "id"; 
	
	public static final String STATUS = "status";
	public static final String QOS    = "qos"; 
}
