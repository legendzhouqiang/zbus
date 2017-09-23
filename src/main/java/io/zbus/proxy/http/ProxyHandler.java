package io.zbus.proxy.http;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import io.zbus.kit.ThreadKit.ManualResetEvent;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.MqClient;
import io.zbus.mq.Protocol;
import io.zbus.transport.Client.ConnectedHandler;
import io.zbus.transport.Client.DisconnectedHandler;
 

public class ProxyHandler implements MessageHandler, Closeable {
	private static final Logger log = LoggerFactory.getLogger(ProxyHandler.class);
	 
	private ProxyHandlerConfig config;
	private final String prefix;  
	
	private Consumer consumer; 
	private ProxyClient detectClient;
	private List<ProxyClient> targetClients; 
	private int currentClient = 0;  
	
	private final ManualResetEvent ready = new ManualResetEvent(false);
	
	
	public ProxyHandler(ProxyHandlerConfig config) {  
		this.config = config; 
		this.prefix = "/" + config.topic; 
	}
	
	private void setupDetectClient(){
		Broker broker = config.broker;
		final AtomicLong connectedTime = new AtomicLong();
		detectClient = new ProxyClient(config.targetServer, broker.getEventLoop(), config.targetHeartbeat);
		detectClient.onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException { 
				detectClient.ensureConnectedAsync();
				new Thread(new Runnable() { //delay check for disconnected by missing heartbeat
					@Override
					public void run() {
						try {
							Thread.sleep(3000); // max connection time, may it be
						} catch (InterruptedException e) {
							return;
						}
						if(detectClient.hasConnected()) return;
						
						ready.reset();
						consumer.pause(); 
					}
				}).start(); 
			}
		});
		
		detectClient.onConnected(new ConnectedHandler() { 
			@Override
			public void onConnected() throws IOException { 
				ready.set();
				consumer.resume();
				connectedTime.set(System.currentTimeMillis());
			}
		}); 
		detectClient.ensureConnectedAsync();
	}

	public synchronized void start() {
		if (consumer != null) return; 
		ConsumerConfig consumeConfig = new ConsumerConfig(config.broker);
		consumeConfig.setTopic(config.topic);
		consumeConfig.setConnectionCount(config.consumerCount);
		consumeConfig.setTopicMask(Protocol.MASK_MEMORY|Protocol.MASK_PROXY);
		consumeConfig.setMaxInFlightMessage(1); //run each time
		consumeConfig.setConsumeTimeout(config.consumeTimeout);
		consumeConfig.setToken(config.token);
		
		consumer = new Consumer(consumeConfig);
		consumer.setMessageHandler(this); 
		try {
			boolean pauseOnStart = true;
			consumer.start(pauseOnStart);
		} catch (IOException e) { 
			log.error(e.getMessage(), e);
			return;
		}
		
		setupDetectClient();
		
		targetClients = new ArrayList<ProxyClient>();
		int targetCount = config.targetClientCount;  
		for(int i=0;i<targetCount;i++){
			ProxyClient client = new ProxyClient(config.targetServer, config.broker.getEventLoop(), config.targetHeartbeat); 
			targetClients.add(client);
		}
		
		for(ProxyClient client : targetClients){ 
			client.ensureConnectedAsync(); 
		}
	}
	

	@Override
	public void handle(Message msg, MqClient client) throws IOException {  
		try {
			ready.await();
		} catch (InterruptedException e) {
			return;
		}
		
		String url = msg.getUrl();

		if (url == null) {
			log.error("missing url");
			return;
		}
		if (url.startsWith(prefix)) {
			url = url.substring(prefix.length());
			if (!url.startsWith("/")) {
				url = "/" + url;
			}
		} else {
			log.error("Url unmatched");
			return;
		} 
		String newUrl = config.targetUrl;
		if(!"/".equals(url)){
			newUrl += url;
		}
		if (!newUrl.startsWith("/")) {
			newUrl = "/" + newUrl;
		}  
		
		msg.setUrl(newUrl); 
		msg.removeHeader(Protocol.TOPIC); 
		int idx = 0; 
		try { 
			if(config.sendFilter != null){
				if(config.sendFilter.filter(msg, client) == false){
					return;
				}
			}
			 
			currentClient = (currentClient+1)%targetClients.size();
			idx = currentClient;
			ProxyClient targetClient = targetClients.get(idx); 
			targetClient.sendMessage(msg, client);   
			
		} catch (Exception e) {
			ProxyClient targetClient = targetClients.get(idx); 
			ProxyClient newClient = new ProxyClient(config.targetServer, config.broker.getEventLoop(), config.targetHeartbeat); 
			targetClients.set(idx, newClient); 
			targetClient.close();
			
			String error = String.format("Target(%s/%s) error: %s", config.targetServer, config.targetUrl, e.toString());
			log.warn(error); 
		}   
	}
	 
	@Override
	public void close() throws IOException {   
		for(ProxyClient client : targetClients){ 
			client.close();
		}
		if (consumer != null) {
			consumer.close();
			consumer = null;
		} 
	}     
}
