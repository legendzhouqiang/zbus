package io.zbus.proxy.http;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
import io.zbus.proxy.http.ProxyConfig.ProxyHandlerConfig;
import io.zbus.transport.Client.ConnectedHandler;
import io.zbus.transport.Client.DisconnectedHandler;
import io.zbus.transport.Session;
 

public class ProxyHandler implements MessageHandler, Closeable {
	protected static final Logger log = LoggerFactory.getLogger(ProxyHandler.class);
	
	private final String topic;
	private final String prefix;
	private final String targetServer;
	private final String targetUrl; 
	private Broker broker; 
	private Consumer consumer; 
	private List<HttpClient> targetClients;
	private int currentClient = 0;  
	
	private final ManualResetEvent ready = new ManualResetEvent(false);
	private ProxyHandlerConfig config;
	
	public ProxyHandler(ProxyHandlerConfig config) {  
		this.config = config;
		this.topic = config.topic;
		this.prefix = "/" + topic;
		this.broker = config.broker; 
		String target = config.targetUrl;
		 
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
	}

	public synchronized void start() {
		if (consumer != null) return; 
		ConsumerConfig consumeConfig = new ConsumerConfig(this.broker);
		consumeConfig.setTopic(topic);
		consumeConfig.setConnectionCount(config.connectionCount);
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
		
		targetClients = new ArrayList<HttpClient>();
		int targetCount = config.connectionCount; //same as consumer's connection count
		if(!config.targetKeepAlive){
			targetCount = 1; //only need one to detect connectivity
		}
		for(int i=0;i<targetCount;i++){
			HttpClient client = new HttpClient(); 
			targetClients.add(client);
		}
		
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
		String newUrl = targetUrl;
		if(!"/".equals(url)){
			newUrl += url;
		}
		if (!newUrl.startsWith("/")) {
			newUrl = "/" + newUrl;
		}  
		
		msg.setUrl(newUrl); 
		msg.removeHeader(Protocol.TOPIC);
		Message res = null;
		try { 
			if(config.sendFilter != null){
				if(config.sendFilter.filter(msg, client) == false){
					return;
				}
			}
			
			if(config.targetKeepAlive){
				currentClient = (currentClient+1)%targetClients.size();
				HttpClient targetClient = targetClients.get(currentClient); 
				targetClient.sendMessage(client, msg); 
			} else {
				shortHttp(client, msg, targetServer);
			} 
			
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
		for(HttpClient client : targetClients){ 
			client.close();
		}
		if (consumer != null) {
			consumer.close();
			consumer = null;
		} 
	}   
	
	
	void shortHttp(MqClient senderClient, Message req, String server) throws IOException {  
		final String msgId = req.getId();
		final String topic = req.getTopic();
		final String sender = req.getSender();
		
		String format = "http://%s%s";
		if(server.startsWith("http://")){
			format = "%s%s";
		}
		String urlString = String.format(format, server, req.getUrl());
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		for(Entry<String, String> e : req.getHeaders().entrySet()){
			conn.setRequestProperty(e.getKey(), e.getValue());
		}
		conn.setRequestMethod(req.getMethod());
		
		if(req.getBody() != null && req.getBody().length > 0){
			conn.setDoOutput(true);
			conn.getOutputStream().write(req.getBody());
		}
		
		int code = conn.getResponseCode();
		Message res = new Message();
		res.setStatus(code);
		
		for (Entry<String, List<String>> header : conn.getHeaderFields().entrySet()){
			String key = header.getKey();
			if(key == null) continue;
			key = key.toLowerCase();
			if(key.equals("transfer-encoding")){ //ignore transfer-encoding
				continue;
			}
			
			List<String> values = header.getValue();
			String value = null;
			if(values.size() == 1){
				value = values.get(0);
			} else if(values.size() > 1){
				value = "[";
				for(int i=0; i<values.size(); i++){
					if(i == value.length()-1){
						value += values.get(i);
					} else {
						value += values.get(i) + ",";
					}
				}
			} 
			res.setHeader(key, value);
		}
		InputStream bodyStream = conn.getInputStream();
		if(bodyStream != null){
			ByteArrayOutputStream sink = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int len = 0;
			while((len=bodyStream.read(buf))>=0){
				sink.write(buf, 0, len);
			}
			res.setBody(sink.toByteArray()); 
		} 
		conn.disconnect(); 

		
		if(config.recvFilter != null){
			if( config.recvFilter.filter(res, senderClient) == false){
				return;
			}
		}
		
		res.setId(msgId);
		res.setTopic(topic);
		res.setReceiver(sender); 
		try {
			senderClient.route(res);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
    
	class HttpClient implements Closeable {
		MqClient client; 
		Queue<Context> requests = new ConcurrentLinkedQueue<Context>();
		Map<String, Context> requestTable = new ConcurrentHashMap<String, Context>();
		
		HttpClient() {   
			client = new MqClient(targetServer, broker.getEventLoop(), config.heartbeatInterval);
			client.onDisconnected(new DisconnectedHandler() {  
				public void onDisconnected() throws IOException { 
					ready.reset();
					consumer.pause();
					client.ensureConnectedAsync();
				}
			});
			
			client.onConnected(new ConnectedHandler() { 
				public void onConnected() throws IOException {  
					ready.set();
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
					
					if(config.recvFilter != null){
						if( config.recvFilter.filter(res, ctx.senderClient) == false){
							return;
						}
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
		
		@Override
		public void close() throws IOException {
			client.close();
		}
		
		class Context{
			String topic;
			String msgId;
			String sender;
			MqClient senderClient;
		}
	}  
}
