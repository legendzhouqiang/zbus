package io.zbus.mq;

import java.io.File;
import java.io.IOException;
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
	
	public String mqBaseDir = "/tmp/zbus";
	
	private Map<String, MessageQueue> mqTable = new ConcurrentHashMap<>();
	
	public MessageQueue get(String mqName) {
		if(mqName == null) mqName = "";
		return mqTable.get(mqName);
	} 
	
	/**
	 * 
	 * Create MQ or Channel of MQ
	 * 
	 * @param mqName name of message queue
	 * @param mqType type of mq
	 * @param channel channel name of mq
	 * @return created/updated mq
	 * @throws IOException 
	 */
	public MessageQueue saveQueue(
			String mqName, String mqType, Integer mqMask, 
			String channel, Long channelOffset, Integer channelMask) throws IOException { 
		
		if(mqName == null) {
			throw new IllegalArgumentException("Missing mqName");
		}
		if(mqType == null) {
			mqType = MEMORY;
		}
		
		MessageQueue mq = mqTable.get(mqName); 
		if(mq == null) {
			if(MEMORY.equals(mqType)) {
				mq = new MemoryQueue(mqName);
			} else if (DISK.equals(mqType)) {
				mq = new DiskQueue(mqName, new File(mqBaseDir));
			} else if(DB.equals(mqName)) {
				mq = new DbQueue(mqName);
			} else {
				throw new IllegalArgumentException("mqType(" + mqType + ") Not Support");
			}  
			mqTable.put(mqName, mq);
		}
		
		mq.setMask(mqMask); 
		
		if(channel != null) {
			Channel ch = new Channel(channel, channelOffset);  
			ch.mask = channelMask;
			mq.saveChannel(ch);
		}
		
		return mq;
	} 
	
	/**
	 * Remove MQ or Channel of MQ
	 * 
	 * @param mq name of mq
	 * @param channel channel of mq
	 */ 
	public void removeQueue(String mq, String channel) throws IOException {
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
