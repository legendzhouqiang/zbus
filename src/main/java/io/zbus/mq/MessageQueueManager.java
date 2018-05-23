package io.zbus.mq;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.db.DbQueue;
import io.zbus.mq.model.disk.DiskQueue;
import io.zbus.mq.model.memory.MemoryQueue;

public class MessageQueueManager {
	public static final String MEMORY = "mem";
	public static final String DISK = "disk";
	public static final String DB = "db";
	
	private Map<String, MessageQueue> mqTable = new ConcurrentHashMap<>();
	
	public MessageQueue get(String mqName) {
		if(mqName == null) mqName = "";
		return mqTable.get(mqName);
	} 
	
	/**
	 * 
	 * Create MQ or Channel of MQ
	 * 
	 * @param mqName
	 * @param mqType
	 * @param channel
	 * @return
	 */
	public MessageQueue createQueue(
			String mqName, String mqType, Long mqMask, 
			String channel, Long channelOffset, Long channelMask) { 
		
		if(mqName == null) {
			throw new IllegalArgumentException("Missing mqName");
		}
		if(mqType == null) {
			mqType = MEMORY;
		}
		
		MessageQueue mq = mqTable.get(mqName); 
		if(mq == null) {
			if(MEMORY.equals(mqType)) {
				mq = new MemoryQueue(mqName, mqMask);
			} else if (DISK.equals(mqType)) {
				mq = new DiskQueue(mqName, mqMask);
			} else if(DB.equals(mqName)) {
				mq = new DbQueue(mqName, mqMask);
			} else {
				throw new IllegalArgumentException("mqType(" + mqType + ") Not Support");
			}  
			mqTable.put(mqName, mq);
		} else {
			mq.setMask(mqMask);
		}
		
		if(channel != null) {
			Channel ch = new Channel(channel, channelMask);  
			ch.offset = channelOffset;
			mq.addChannel(ch);
		}
		
		return mq;
	} 
	
	/**
	 * Remove MQ or Channel of MQ
	 * 
	 * @param mq
	 * @param channel
	 */ 
	public void removeQueue(String mq, String channel) {
		if(channel == null) {
			MessageQueue q = mqTable.remove(mq);
			if(q != null) {
				q.destroy();
			}
			return;
		}
		
		MessageQueue q = mqTable.get(mq);
		if(q != null) {
			q.removeChannel(channel);
		}
	} 
}
