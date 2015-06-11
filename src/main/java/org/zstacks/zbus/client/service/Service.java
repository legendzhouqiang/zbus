package org.zstacks.zbus.client.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zstacks.zbus.client.Consumer;
import org.zstacks.znet.Message;

public class Service implements Closeable {   
	private static final Logger log = LoggerFactory.getLogger(Service.class);
	private final ServiceConfig config; 
	private ConsumerThread[] consumerThreads;
	private final ThreadPoolExecutor threadPoolExecutor;
	
	public Service(ServiceConfig config){
		this.config = config;
		if(config.getMq() == null || "".equals(config.getMq())){
			throw new IllegalArgumentException("MQ required");
		}
		if(config.getServiceHandler() == null){
			throw new IllegalArgumentException("serviceHandler required");
		}  
		threadPoolExecutor = new ThreadPoolExecutor(config.getConsumerCount(),
				4*config.getConsumerCount(), 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	 
	@Override
	public void close() throws IOException {
		if(this.consumerThreads != null){
			for(ConsumerThread thread : this.consumerThreads){
				try {
					thread.close();
				} catch (IOException e) {
					log.debug(e.getMessage(), e);
				}
			}
		} 
	}
	
	public void start(){    
		this.consumerThreads = new ConsumerThread[config.getConsumerCount()];
		for(int i=0;i<consumerThreads.length;i++){
			@SuppressWarnings("resource")
			ConsumerThread thread = new ConsumerThread(config, threadPoolExecutor); 
			this.consumerThreads[i] = thread; 
			this.consumerThreads[i].start();
		}
	} 
}



class ConsumerThread extends Thread implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(ConsumerThread.class);
	private ServiceConfig config = null;  
	private Consumer consumer;
	private ThreadPoolExecutor threadPoolExecutor;
	public ConsumerThread(ServiceConfig config, final ThreadPoolExecutor threadPoolExecutor){ 
		this.config  = config;  
		this.threadPoolExecutor = threadPoolExecutor;
	}  
	
	@Override
	public void run() { 
		this.consumer = new Consumer(config);
		final int timeout = config.getReadTimeout(); //ms  
		
		while(!isInterrupted()){
			try {  
				final Message msg = consumer.recv(timeout); 
				if(msg == null) continue;
				
				if(log.isDebugEnabled()){
					log.debug("Request: {}", msg);
				}
				
				final String mqReply = msg.getMqReply();
				final String msgId  = msg.getMsgIdRaw(); //必须使用原始的msgId
				if(threadPoolExecutor == null){
					break;
				}
				
				threadPoolExecutor.submit(new Runnable() { 
					@Override
					public void run() {
						Message res = config.getServiceHandler().handleRequest(msg); 
						if(res != null){ 
							res.setMsgId(msgId); 
							res.setMq(mqReply);		
							try {
								consumer.reply(res);
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
						}  	
					}
				});
				
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
		this.threadPoolExecutor.shutdown();
		this.threadPoolExecutor = null;
		this.interrupt(); 
	}
}

