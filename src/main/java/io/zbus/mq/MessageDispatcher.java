package io.zbus.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;
import io.zbus.transport.Session;
import io.zbus.transport.Session.SessionType;
import io.zbus.transport.http.HttpMessage;

public class MessageDispatcher {
	private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);

	private SubscriptionManager subscriptionManager;
	private Map<String, Session> sessionTable;
	private Map<String, Long> loadbalanceTable = new ConcurrentHashMap<String, Long>(); // channel => index

	private int batchReadSize = 10;
	private ExecutorService dispatchRunner = Executors.newFixedThreadPool(64);

	public MessageDispatcher(SubscriptionManager subscriptionManager, Map<String, Session> sessionTable) {
		this.subscriptionManager = subscriptionManager;
		this.sessionTable = sessionTable;
	}

	public void dispatch(MessageQueue mq, String channel) {
		dispatchRunner.submit(() -> {
			dispatch0(mq, channel);
		});
	}

	protected void dispatch0(MessageQueue mq, String channel) {
		List<Subscription> subs = subscriptionManager.getSubscriptionList(mq.name(), channel);
		if (subs == null || subs.size() == 0)
			return;

		synchronized (subs) {
			Long index = loadbalanceTable.get(channel);
			if (index == null) {
				index = 0L;
			}
			while (true) {
				List<Map<String, Object>> data;
				try {
					data = mq.read(channel, batchReadSize);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					break;
				}
				for (Map<String, Object> message : data) {
					String filter = (String) message.get(Protocol.TOPIC);
					int N = subs.size();
					long max = index + N;
					while (index < max) {
						Subscription sub = subs.get((int) (index % N));
						index++;
						if (index < 0)
							index = 0L;
						if (sub.topics.isEmpty() || sub.topics.contains(filter)) {
							Session sess = sessionTable.get(sub.clientId);
							if (sess == null)
								continue;
							message.put(Protocol.CHANNEL, channel);
							message.put(Protocol.SENDER, sess.id());
							sendMessage(message, sess);
							break;
						}
					}
				}
				if (data.size() < batchReadSize)
					break;
			}
			loadbalanceTable.put(channel, index);
		}
	}

	public void dispatch(MessageQueue mq) {
		Iterator<String> iter = mq.channelIterator();
		while(iter.hasNext()) {
			String channel = iter.next();
			dispatch(mq, channel);
		} 
	}

	public void take(MessageQueue mq, String channel, int count, String reqMsgId, Session sess) throws IOException { 
		List<Map<String, Object>> data = mq.read(channel, count);
		Map<String, Object> message = new HashMap<>();  
		message.put(Protocol.STATUS, 200);
		if(count <= 1) {
			if(data.size() >= 1) { 
				message = data.get(0); 
				message.put(Protocol.STATUS, 200);
			} else {
				message.put(Protocol.STATUS, 604); //Special status code, no DATA
			}
		} else {
			message.put(Protocol.BODY, data); //batch message format
		}
		message.put(Protocol.ID, reqMsgId);
		message.put(Protocol.MQ, mq.name());
		message.put(Protocol.CHANNEL, channel);
		message.put(Protocol.SENDER, sess.id());
		sendMessage(message, sess);
	}
	

	public void sendMessage(Map<String, Object> data, Session sess) { 
		SessionType clientType = sess.attr(Session.TYPE_KEY); //Get type of session
		if(clientType == null) clientType = SessionType.Websocket; 
		
		if (clientType == SessionType.Websocket) {
			sess.write(JSON.toJSONBytes(data));
			return;
		}
		
		if (clientType == SessionType.Inproc) {
			sess.write(data);
			return;
		} 
		
		if (clientType == SessionType.HTTP) {
			HttpMessage res = new HttpMessage();
			Integer status = (Integer) data.get(Protocol.STATUS);
			if (status == null) status = 200;
			res.setStatus(status);
			res.setJsonBody(JSON.toJSONBytes(data));
	
			sess.write(res);
			
			return;
		}
	}
}
