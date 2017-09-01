package io.zbus.proxy.http;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
import io.zbus.transport.Session;
 

public class ProxyHandler implements MessageHandler, Closeable {
	protected static final Logger log = LoggerFactory.getLogger(ProxyHandler.class);
	
	private final String entry;
	private final String prefix;
	private final String targetServer;
	private final String targetUrl;
	private int connectionCount;
	private Broker broker; 
	private Consumer consumer; 
	private List<HttpClient> targetClients;
	private int currentClient = 0;  
	private final AtomicReference<CountDownLatch> ready = new AtomicReference<CountDownLatch>(new CountDownLatch(1));
	private Thread readyThread;
	public ProxyHandler(String entry, String target, Broker broker, int connectionCount) {
		this.connectionCount = connectionCount;
		this.entry = entry;
		this.prefix = "/" + entry;
		this.broker = broker;
		 
		if(target.startsWith("http://")){
			target  = target.substring("http://".length());
		}
		String[] bb = target.split("[//]",2);
		this.targetServer = bb[0].trim();
		String url = "";
		if(bb.length>1){
			url = bb[1].trim();
		} 
		this.targetUrl = url; 
		
		targetClients = new ArrayList<HttpClient>();
		for(int i=0;i<this.connectionCount;i++){
			HttpClient client = new HttpClient(); 
			targetClients.add(client);
		}
	}

	public synchronized void start() {
		if (consumer != null) return; 
		ConsumerConfig config = new ConsumerConfig(this.broker);
		config.setTopic(entry);
		config.setConnectionCount(this.connectionCount);
		config.setTopicMask(Protocol.MASK_MEMORY);
		config.setMaxInFlightMessage(1); //run each time
		consumer = new Consumer(config);
		consumer.setMessageHandler(this);
		
		//wait for downstream ready to start
		readyThread = new Thread(new Runnable() { 
			public void run() { 
				try {
					ready.get().await();
				} catch (InterruptedException e) { 
					return;
				} 
				try { 
					consumer.start();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}); 
		
		readyThread.start();
		
		for(HttpClient client : targetClients){ 
			try {
				client.ensureConnectedAsync();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			} 
		}
	}
	

	@Override
	public void handle(Message msg, MqClient client) throws IOException {  
		try {
			ready.get().await();
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
		}  
		url = targetUrl + url;
		if (!url.startsWith("/")) {
			url = "/" + url;
		}
		
		msg.setUrl(url);
		
		Message res = null;
		try { 
			currentClient = (currentClient+1)%connectionCount;
			HttpClient targetClient = targetClients.get(currentClient);
			targetClient.sendMessage(client, msg); 
		} catch (Exception e) {
			res = new Message();
			if (e instanceof FileNotFoundException) {
				res.setStatus(404);
				res.setBody(e.getMessage() + " Not Found");
			} else {
				res.setStatus(500);
				String error = String.format("Target(%s/%s) invoke error, reason: %s", targetServer, targetUrl, e.toString());
				res.setBody(error);
			}
		}   
	}
	 
	@Override
	public void close() throws IOException {
		if (consumer != null) {
			consumer.close();
			consumer = null;
		} 
	}   
    
	class HttpClient {
		MqClient client; 
		Queue<Context> requests = new ConcurrentLinkedQueue<Context>();
		Map<String, Context> requestTable = new ConcurrentHashMap<String, Context>();
		
		HttpClient() { 
			client = new MqClient(targetServer, broker.getEventLoop());
			client.onDisconnected(new DisconnectedHandler() {  
				public void onDisconnected() throws IOException { 
					ready.set(new CountDownLatch(1));
					consumer.pause();
				}
			});
			
			client.onConnected(new ConnectedHandler() { 
				public void onConnected() throws IOException {  
					ready.get().countDown();
					consumer.resume();
				}
			}); 
			
			client.onMessage(new io.zbus.transport.MessageHandler<Message>() { 
				@Override
				public void handle(Message res, Session session) throws IOException {
					String msgId = res.getId(); 
					Context ctx = null;
					if(msgId != null){
						ctx = requestTable.remove(msgId);
					}  
					if(ctx != null){
						requests.remove(ctx);
					} else { //MsgId not set
						ctx = requests.poll();
						if(ctx != null) {
							Iterator<Entry<String, Context>> iter = requestTable.entrySet().iterator();
							while(iter.hasNext()){
								Entry<String, Context> e = iter.next();
								if(e.getValue() == ctx){
									iter.remove();
									break;
								}
							}
						}
					}
					
					if(ctx == null){ 
						return; //ignore
					}
					
					res.setId(ctx.msgId);
					res.setTopic(ctx.topic);
					res.setReceiver(ctx.sender); 
					try {
						ctx.senderClient.route(res);
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
			});
		}
		
		void sendMessage(MqClient senderClient, Message msg) throws IOException, InterruptedException{
			Context ctx = new Context();
			ctx.msgId = msg.getId();
			ctx.topic = msg.getTopic();
			ctx.sender = msg.getSender();
			ctx.senderClient = senderClient;
			
			requests.add(ctx);
			requestTable.put(ctx.msgId, ctx);
			
			client.sendMessage(msg); 
		}
		
		void ensureConnectedAsync() throws IOException{
			client.ensureConnectedAsync();
		}
		
		class Context{
			String topic;
			String msgId;
			String sender;
			MqClient senderClient;
		}
	} 
}
