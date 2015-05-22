package org.zstacks.zbus.client.service;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zstacks.zbus.client.Consumer;
import org.zstacks.znet.Message;

public class Service implements Closeable {   
	private static final Logger log = LoggerFactory.getLogger(Service.class);
	private final ServiceConfig config; 
	private WorkerThread[] workerThreads;
	
	public Service(ServiceConfig config){
		this.config = config;
		if(config.getMq() == null || "".equals(config.getMq())){
			throw new IllegalArgumentException("MQ required");
		}
		if(config.getServiceHandler() == null){
			throw new IllegalArgumentException("serviceHandler required");
		}  
	}
	 
	@Override
	public void close() throws IOException {
		if(this.workerThreads != null){
			for(WorkerThread thread : this.workerThreads){
				try {
					thread.close();
				} catch (IOException e) {
					log.debug(e.getMessage(), e);
				}
			}
		} 
	}
	
	public void start(){    
		this.workerThreads = new WorkerThread[config.getThreadCount()];
		for(int i=0;i<workerThreads.length;i++){
			@SuppressWarnings("resource")
			WorkerThread thread = new WorkerThread(config); 
			this.workerThreads[i] = thread; 
			this.workerThreads[i].start();
		}
	} 
}



class WorkerThread extends Thread implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(WorkerThread.class);
	private ServiceConfig config = null;  
	private Consumer consumer;
	public WorkerThread(ServiceConfig config){ 
		this.config  = config;  
	}  
	
	@Override
	public void run() { 
		this.consumer = new Consumer(config);
		final int timeout = config.getReadTimeout(); //ms  
		
		while(!isInterrupted()){
			try {  
				Message msg = consumer.recv(timeout); 
				if(msg == null) continue;
				
				if(log.isDebugEnabled()){
					log.debug("Request: {}", msg);
				}
				
				final String mqReply = msg.getMqReply();
				final String msgId  = msg.getMsgIdRaw(); //必须使用原始的msgId
				
				Message res = config.getServiceHandler().handleRequest(msg);
				
				if(res != null){ 
					res.setMsgId(msgId); 
					res.setMq(mqReply);		
					consumer.reply(res);
				}  	
			} catch (InterruptedException e) { 
				break;
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
			}
		} 
		log.info("Service thread({}) closed", this.getId());
		if(this.consumer != null){
			try {
				consumer.close();
				this.consumer = null;
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		} 
	}
	
	@Override
	public void close() throws IOException {
		this.interrupt();
	}
}

