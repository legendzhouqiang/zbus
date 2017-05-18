package io.zbus.mq.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Message;
import io.zbus.net.Session;


public class MessageTracer implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(MessageTracer.class);
	private static final String TOPIC_SET_KEY = "trace_topic_set";
	
	private final int traceMaxQueueLength = 100;
	private final Message[] messages = new Message[traceMaxQueueLength];
	private Set<Session> sessions = new HashSet<Session>();
	private AtomicLong writeIndex = new AtomicLong(-1);
	private AtomicLong readIndex = new AtomicLong(-1);

	private final ReentrantLock lock;  
	private final ReentrantLock sessionLock;  
    private final Condition notEmpty;  
    
    private Thread thread;
    
    public MessageTracer(){
    	lock = new ReentrantLock();
    	sessionLock = new ReentrantLock();
    	notEmpty = lock.newCondition(); 
    }
    
	public void publish(Message message) throws InterruptedException{
		lock.lockInterruptibly();
		try {
			long w = writeIndex.incrementAndGet();
			if(w<0){
				writeIndex.set(0);
			}
			w %= traceMaxQueueLength;
			messages[(int)w] = message; 
			
			notEmpty.signal();
		} finally {
			lock.unlock();
		}
	}
	
	public void subscribe(Message message, Session session){
		Integer flag = message.getTopicMask();
		if(flag != null && flag == 0){//flag 0 means unsubscribe
			cleanSession(session);
			return;
		}
		
		sessionLock.lock();
		try{
			String topicList = message.getTopic(); 
			if(topicList != null && !topicList.trim().isEmpty()){
				Set<String> topicSet = new HashSet<String>();
				for(String topic : topicList.split("[ ,;]")){
					topic = topic.trim();
					if(topic.equals("")) continue;
					topicSet.add(topic);
				}
				session.attr(TOPIC_SET_KEY, topicSet);
			}
			
			Set<Session> workSession = new HashSet<Session>(sessions);
			workSession.add(session);
			sessions = workSession;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			sessionLock.unlock();
		} 
	}
	
	public void cleanSession(Session session){
		sessionLock.lock();
		try{
			Set<Session> workSession = new HashSet<Session>(sessions);
			workSession.remove(session);
			sessions = workSession;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			sessionLock.unlock();
		} 
	}
	
	private Message take() throws InterruptedException {
		lock.lockInterruptibly();
        try {
        	while(readIndex.get() >= writeIndex.get()){
        		notEmpty.await();
    		} 
        	if(readIndex.get()+traceMaxQueueLength < writeIndex.get()){
    			readIndex.set(writeIndex.get()-traceMaxQueueLength);
    		}
    		long r = readIndex.incrementAndGet();
    		r %= traceMaxQueueLength;
    		return messages[(int)r];
        } finally {
            lock.unlock();
        } 
	} 
	
	public synchronized void start(){
		if(thread != null) return;
		
		thread = new Thread(new Runnable() { 
			@Override
			public void run() { 
				while (true) { 
					Message message;
					try {
						message = take();
					} catch (InterruptedException e) {
						break;
					}  
					String topic = message.getTopic();
					message = Message.copyWithoutBody(message); //modify message 
					for(Session session : sessions){
						try{
							Set<String> topicSet = session.attr(TOPIC_SET_KEY); 
							if(message.getStatus() == null){ 
								message.setOriginUrl(message.getUrl());
								message.setStatus(200);
							} 
							if(topicSet == null || topicSet.contains(topic)){
								session.writeAndFlush(message);
							}
						} catch (Exception e) { 
							log.error(e.getMessage(), e);
						}
					} 
				} 
			}
		});
		
		thread.start();
	}
	
	@Override
	public void close() throws IOException { 
		thread.interrupt();
	}   
}
