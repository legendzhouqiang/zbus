package org.zbus.proxy;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.kit.ConfigKit;
import org.zbus.kit.log.Logger;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.http.Message;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

/**
 * HttpDmzProxy works like Nginx/Apache's proxy from first sight, but the underlying mechanism is totally different:
 * Nginx/Apache actively connect to target server/server list
 * HttpDmzProxy is always deployed on the side of target server, and actively connect to ZBUS.
 * 
 * HttpDmzProxy consumes zbus's message produced from the original browser, and actively invoke request 
 * receive response from the target server, which is valid since it resides in the same side of the target server.
 * 
 * The underlying network environment is usually called DMZ(DeMilitarized Zone).
 * 
 * @author rushmore (洪磊明)
 *
 */
public class HttpDmzProxy implements ConsumerHandler, Closeable {
	private static final Logger log = Logger.getLogger(HttpDmzProxy.class); 
	
	private final String entry;
	private final String prefix;
	private final String target;
	private int consumerCount = 4;
	private Broker broker;
	private boolean ownBroker = false;
	public HttpDmzProxy(String brokerAddress, String entry, String target) throws IOException {
		this(new ZbusBroker(brokerAddress), entry, target);
		ownBroker = true;
	}
	
	public HttpDmzProxy(Broker broker, String entry, String target) throws IOException {
		this.broker = broker;
		this.entry = entry;
		this.target = target;
		this.prefix = "/" + entry;
	}
	
	@Override
	public void handle(Message msg, final Consumer consumer) throws IOException {
		final String mq = msg.getMq();
		final String msgId = msg.getId();
		final String sender = msg.getSender();
		String url = msg.getHead("origin-url"); //zbus added this key-value
		
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
		msg.removeHead(Message.MQ);
		msg.removeHead(Message.ID);
		msg.removeHead(Message.SENDER);
		msg.removeHead("origin-url");
		msg.removeHead(Message.SERVER);

		msg.setUrl(url);
		msg.setServer(target);

		Message res = null;
		try {
			res = msg.invokeSimpleHttp();
		} catch (IOException e) {
			res = new Message();
			if (e instanceof FileNotFoundException) {
				res.setStatus(404);
				res.setBody(e.getMessage() + " Not Found");
			} else {
				res.setStatus(500);
				res.setBody(e.toString());
			}
		}
		
		res.setId(msgId);
		res.setMq(mq);
		res.setRecver(sender);
		// route back message
		try {
			consumer.routeMessage(res);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}

	}
	
	
	
	private Service service;
	public void start() throws IOException{
		if(service != null) return;
		
		if(broker == null){
			throw new IllegalStateException("missing broker");
		}
		ServiceConfig config = new ServiceConfig();
		config.setConsumerCount(consumerCount);
		config.setMq(entry);
		config.setBroker(broker);
		config.setConsumerHandler(this);
		config.setMode(MqMode.MQ, MqMode.Memory);
		
		service = new Service(config);
		service.start();
	}
	
	@Override
	public void close() throws IOException {
		if(service != null){
			service.close();
			service = null;
		}
		if(ownBroker && broker != null){
			broker.close(); 
			broker = null;
		}
	}

	
	
	public int getConsumerCount() {
		return consumerCount;
	}

	public void setConsumerCount(int consumerCount) {
		this.consumerCount = consumerCount;
	} 
	
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		String target = ConfigKit.option(args, "-t", "127.0.0.1:8080");
		String entry = ConfigKit.option(args, "-mq", "http-ws");
		String brokerAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		
		HttpDmzProxy proxy = new HttpDmzProxy(brokerAddress, entry, target); 		
		proxy.start(); 
	} 
}
