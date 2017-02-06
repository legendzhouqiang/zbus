package io.zbus.mq.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.zbus.mq.Message;
import io.zbus.net.Session;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;
  
public class TraceService implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(TraceService.class);
	private final int traceMaxQueueLength = 100;
	private final Message[] messages = new Message[traceMaxQueueLength];
	private Set<Session> sessions = new HashSet<Session>();
	private AtomicLong writeIndex = new AtomicLong(-1);
	private AtomicLong readIndex = new AtomicLong(-1);

	private final ReentrantLock lock;  
	private final ReentrantLock sessionLock;  
    private final Condition notEmpty;  
    
    private Thread thread;
    
    public TraceService(){
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
	
	public void subscribe(Session session){
		sessionLock.lock();
		try{
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
					
					message = Message.copyWithoutBody(message); //modify message 
					for(Session session : sessions){
						try{
							message.setStatus(200);
							session.writeAndFlush(message);
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
