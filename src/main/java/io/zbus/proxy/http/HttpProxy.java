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

import io.zbus.kit.ConfigKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.MqClient;
import io.zbus.mq.Protocol;
import io.zbus.transport.Session;

/**
 * HttpProxy works like Nginx/Apache's proxy at first sight, but the
 * underlying traffic is totally different: Nginx/Apache actively connect to
 * target server, HttpProxy is always deployed on the side of
 * target server, and actively connect to zbus broker. 
 * 
 * The underlying network environment is usually called DMZ(DeMilitarized Zone).
 * 
 * @author rushmore (洪磊明)
 *
 */

public class HttpProxy implements MessageHandler, Closeable {
	protected static final Logger log = LoggerFactory.getLogger(HttpProxy.class);
	
	private final String entry;
	private final String prefix;
	private final String target;
	private int connectionCount;
	private Broker broker;
	private boolean ownBroker = false;
	private Consumer consumer; 
	private List<HttpClient> targetClients;
	private int currentClient = 0;

	public HttpProxy(ProxyConfig config) throws IOException {
		this.connectionCount = config.connectionCount;
		this.entry = config.entry;
		this.prefix = "/" + entry;
		this.target = config.target;
		if (config.broker != null) {
			this.broker = config.broker;
		} else {
			if (config.brokerConfig == null) {
				throw new IllegalArgumentException("Missing broker config");
			}
			this.broker = new Broker(config.brokerConfig);
			this.ownBroker = true;
		} 
		targetClients = new ArrayList<HttpClient>();
		for(int i=0;i<this.connectionCount;i++){
			targetClients.add(new HttpClient());
		}
	}

	public synchronized void start() throws IOException {
		if (consumer != null) return;
		ConsumerConfig config = new ConsumerConfig(this.broker);
		config.setTopic(entry);
		config.setConnectionCount(this.connectionCount);
		config.setTopicMask(Protocol.MASK_MEMORY);
		consumer = new Consumer(config);
		consumer.setMessageHandler(this);

		consumer.start();
	}

	@Override
	public void handle(Message msg, MqClient client) throws IOException { 
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
				String error = String.format("Target(%s) invoke error, reason: %s", target, e.toString());
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
		if (ownBroker && this.broker != null) {
			this.broker.close();
			this.broker = null;
		}
	} 
	
    
    
	class HttpClient {
		MqClient client; 
		Queue<Context> requests = new ConcurrentLinkedQueue<Context>();
		Map<String, Context> requestTable = new ConcurrentHashMap<String, Context>();
		
		HttpClient() { 
			client = new MqClient(target, broker.getEventLoop());
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
		
		class Context{
			String topic;
			String msgId;
			String sender;
			MqClient senderClient;
		}
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		ProxyConfig config = new ProxyConfig();
		config.entry = ConfigKit.option(args, "-mq", "http");
		config.target = ConfigKit.option(args, "-t", "127.0.0.1:80");
		String address = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		config.brokerConfig = new BrokerConfig();
		config.brokerConfig.addTracker(address);

		HttpProxy proxy = new HttpProxy(config);
		proxy.start();
	}
}
