package org.zbus.server.mq.store;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.zbus.common.MessageMode;
import org.zbus.common.MqInfo;
import org.zbus.common.json.JSON;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.Message;
import org.zbus.remoting.MessageCodec;
import org.zbus.remoting.nio.IoBuffer;
import org.zbus.server.mq.MessageQueue;
import org.zbus.server.mq.RequestQueue;

import redis.clients.jedis.Jedis;
 
public class MessageStoreRedis implements MessageStore {
	private static final Logger log = LoggerFactory.getLogger(MessageStoreRedis.class);private Jedis jedis;

	private static final MessageCodec codec = new MessageCodec();
	private static final String ZBUS_PREFIX = "zbus-";
	private final String brokerKey;
	
	public MessageStoreRedis(Jedis jedis, String broker){
		this.jedis = jedis;
		this.brokerKey = ZBUS_PREFIX + broker;
	}
	
	private String mqKey(String mq){
		return String.format("%s-%s", brokerKey, mq);
	}
	
	@Override
	public void saveMessage(Message msg) {  
		String msgKey = msg.getMsgId();
		String mqKey = mqKey(msg.getMq());  
		jedis.set(msgKey, msg.toString());  
		jedis.rpush(mqKey, msgKey);
	}

	@Override
	public void removeMessage(Message msg) {
		String msgKey = msg.getMsgId();
		String mqKey = mqKey(msg.getMq());  
		
		jedis.del(msgKey);  
		jedis.lrem(mqKey, 1, msgKey); 
	}
	
	@Override
	public void onMessageQueueCreated(MessageQueue mq) { 
		String json = JSON.toJSONString(mq.getMqInfo());
		jedis.hset(this.brokerKey, mq.getName(), json);
	}
	
	@Override
	public void onMessageQueueRemoved(MessageQueue mq) { 
		jedis.hdel(this.brokerKey, mq.getName());
	}
	
	@Override
	public ConcurrentMap<String, MessageQueue> loadMqTable() { 
		Map<String, String> mqs = jedis.hgetAll(this.brokerKey);
		ConcurrentHashMap<String, MessageQueue> res = new ConcurrentHashMap<String, MessageQueue>();
		Iterator<Entry<String, String>> iter = mqs.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, String> e = iter.next();
			String mqName = e.getKey();
			String mqInfoString = e.getValue();
			MqInfo info = JSON.parseObject(mqInfoString, MqInfo.class);
			int mode = info.getMode();
			if(!MessageMode.isEnabled(mode, MessageMode.MQ)){
				log.warn("message queue mode not support");
				continue;
			} 
			
			RequestQueue mq = new RequestQueue(info.getBroker(),
					mqName, null, mode);
			mq.setCreator(info.getCreator());
			
			String mqKey = mqKey(mqName);
			
			//TODO batch
			List<String> msgIds = jedis.lrange(mqKey, 0, -1); 
			if(msgIds.size() == 0) continue;
			
			List<String> msgStrings = jedis.mget(msgIds.toArray(new String[0]));
			List<Message> msgs = new ArrayList<Message>();
			for(String msgString : msgStrings){
				IoBuffer buf = IoBuffer.wrap(msgString);
				Message msg = (Message) codec.decode(buf);
				if(msg != null){
					msgs.add(msg);
				} else {
					log.error("message decode error");
				}
			}
			mq.loadMessageList(msgs);
			res.put(mqName, mq);
		}
		return res;
	}
	
	@Override
	public void start() { 
		
	}
	
	@Override
	public void shutdown() { 
		
	}
	
	public static void main(String[] args){
		Jedis jedis = new Jedis("localhost");
		jedis.del("list1");
		Message msg = new Message();
		msg.setStatus("200");
		jedis.rpush("list1", "hong", "lei", "ming");
		List<String> res = jedis.lrange("list1", 0, 10);
		for(String x : res){
			System.out.println(x);
		}
		jedis.lrem("list1", 1, "lei");
		
		res = jedis.lrange("list1", 0, 10);
		for(String x : res){
			System.out.println(x);
		}
		
		jedis.close();
	}
}
