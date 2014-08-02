package org.zbus.client.rpc;

import java.io.IOException;

import org.logging.Logger;
import org.logging.LoggerFactory;
import org.remoting.ClientDispachterManager;
import org.remoting.Message;
import org.remoting.RemotingClient;
import org.zbus.client.Consumer;

class WorkerThread extends Thread{
	private static final Logger log = LoggerFactory.getLogger(WorkerThread.class);
	private RpcServiceConfig config = null;
	private final ClientDispachterManager manager;   
	
	public WorkerThread(ClientDispachterManager manager, RpcServiceConfig config){
		this.manager = manager;
		this.config  = config; 
		
	} 
	
	@Override
	public void run() {
		if(config.getBroker() == null && config.getClientBuilder() == null){
			throw new IllegalStateException("Both broker and ClientBuilder are missing in config");
		} 
		Consumer consumer = null;
		
		if(config.getClientBuilder() != null){ 
			consumer = new Consumer(config.getClientBuilder(), config.getServiceName());
		} else {
			RemotingClient client = new RemotingClient(config.getBroker(), manager);
			consumer = new Consumer(client, config.getServiceName());
		}
		
		final int timeout = config.getConsumeTimeout(); //ms 
		consumer.setAccessToken(config.getAccessToken());
		consumer.setRegisterToken(config.getRegisterToken());
		
		while(true){
			try {  
				Message msg = consumer.recv(timeout); 
				if(msg == null) continue;  
				log.debug("Request: %s", msg);
				
				final String mqReply = msg.getMqReply();
				final String msgId  = msg.getMsgIdRaw(); //必须使用原始的msgId
				
				Message res = config.getServiceHandler().handleRequest(msg);
				
				if(res != null){ 
					res.setMsgId(msgId); 
					res.setMq(mqReply);		
					consumer.reply(res);
				} 
				
			} catch (IOException e) { 
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) { 
					//ignore
				}
			}
		}
	}
}

public class RpcService extends Thread {   
	private static final Logger log = LoggerFactory.getLogger(RpcService.class);
	private final RpcServiceConfig config;
	private ClientDispachterManager clientMgr;  
	private Thread[] workerThreads;
	
	public RpcService(RpcServiceConfig config) throws IOException{
		this.config = config;
		if(config.getServiceName() == null || "".equals(config.getServiceName())){
			throw new IllegalArgumentException("servieName required");
		}
		if(config.getServiceHandler() == null){
			throw new IllegalArgumentException("serviceHandler required");
		} 
		clientMgr = new ClientDispachterManager();
	}
	
	public void run(){  
		clientMgr.start();
		
		this.workerThreads = new Thread[config.getThreadCount()];
		for(int i=0;i<workerThreads.length;i++){
			WorkerThread thread = new WorkerThread(clientMgr, config); 
			this.workerThreads[i] = thread; 
			this.workerThreads[i].start();
		}
		
		for(Thread thread : this.workerThreads){
			try {
				thread.join();
			} catch (InterruptedException e) { 
				log.error(e.getMessage(), e);
			}
		}
	} 
}
