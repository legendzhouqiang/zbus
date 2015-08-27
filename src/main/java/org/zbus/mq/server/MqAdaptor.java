/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.mq.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractQueue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.zbus.kit.ConfigKit;
import org.zbus.kit.FileKit;
import org.zbus.kit.JsonKit;
import org.zbus.kit.NetKit;
import org.zbus.log.Logger;
import org.zbus.mq.Protocol;
import org.zbus.mq.Protocol.BrokerInfo;
import org.zbus.mq.Protocol.MqInfo;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.mq.server.support.DiskQueuePool;
import org.zbus.mq.server.support.DiskQueuePool.DiskQueue;
import org.zbus.mq.server.support.MessageDiskQueue;
import org.zbus.mq.server.support.MessageMemoryQueue;
import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageCodec;

public class MqAdaptor extends IoAdaptor {
	private static final Logger log = Logger.getLogger(MqAdaptor.class);

	private final Map<String, AbstractMQ> mqTable = new ConcurrentHashMap<String, AbstractMQ>();
	private final Map<String, Session> sessionTable = new ConcurrentHashMap<String, Session>();
	private final Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<String, MessageHandler>();
	
	private boolean verbose = false;   
	private final String serverAddr;
	
	private final ScheduledExecutorService cleanExecutor = Executors.newSingleThreadScheduledExecutor();
	private long cleanInterval = 3000; 
 
	public MqAdaptor(String serverAddr){
		codec(new MessageCodec());   
		this.serverAddr = NetKit.getLocalAddress(serverAddr);
		
		registerHandler(Protocol.Produce, produceHandler); 
		registerHandler(Protocol.Consume, consumeHandler);  
		registerHandler(Protocol.Route, routeHandler);  
		registerHandler(Protocol.CreateMQ, createMqHandler);  
		registerHandler(Protocol.Admin, new AdminHandler()); 
		registerHandler(Message.HEARTBEAT, heartbeatHandler);  
		
		this.cleanExecutor.scheduleAtFixedRate(new Runnable() { 
			public void run() {  
				Iterator<Entry<String, AbstractMQ>> iter = mqTable.entrySet().iterator();
		    	while(iter.hasNext()){
		    		Entry<String, AbstractMQ> e = iter.next();
		    		AbstractMQ mq = e.getValue(); 
		    		mq.cleanSession();
		    	}
			}
		}, 1000, cleanInterval, TimeUnit.MILLISECONDS); 
	} 
    
    public void onMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	msg.setSender(sess.id());
		msg.setBroker(serverAddr); 
		msg.setRemoteAddr(sess.getRemoteAddress());
		
		if(verbose){
			log.info("\n%s", msg);
		}
		
		String cmd = msg.getCmd(); 
		if(cmd == null){ //default to Admin
			cmd = Protocol.Admin;
		}
    	
    	MessageHandler handler = handlerMap.get(cmd);
    	if(handler != null){
    		handler.handle(msg, sess);
    		return;
    	}
    	
    	Message res = new Message();
    	res.setId(msg.getId()); 
    	res.setResponseStatus(400);
    	String text = String.format("Bad format: command(%s) not support", cmd);
    	res.setBody(text); 
    	sess.write(res); 
    } 
	
    private AbstractMQ findMQ(Message msg, Session sess) throws IOException{
		String mqName = msg.getMq();
		AbstractMQ mq = mqTable.get(mqName); 
    	if(mq == null){
    		ReplyKit.reply404(msg, sess); 
    		return null;
    	} 
    	return mq;
	}
     
    public void registerHandler(String command, MessageHandler handler){
    	this.handlerMap.put(command, handler);
    } 
    
	private MessageHandler produceHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			AbstractMQ mq = findMQ(msg, sess);
			if(mq == null) return;
			boolean ack = msg.isAck();
			msg.removeHead(Message.CMD);
			msg.removeHead(Message.ACK);
			
			mq.produce(msg, sess);
			mq.lastUpdateTime = System.currentTimeMillis();
			if(ack){
				ReplyKit.reply200(msg, sess);
			}
		}
	}; 
	
	private MessageHandler consumeHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			AbstractMQ mq = findMQ(msg, sess);
			if(mq == null) return;
			mq.consume(msg, sess);
		}
	}; 
	
	private MessageHandler routeHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			String recver = msg.getRecver();
			if(recver == null) {
				return; //just igmore
			}
			Session target = sessionTable.get(recver);
			if(target == null) {
				log.warn("Missing target %s", recver); 
				return; //just ignore
			} 
			msg.removeHead(Message.ACK);
			msg.removeHead(Message.RECVER);
			msg.removeHead(Message.CMD);
			target.write(msg);
		}
	}; 
	
	private MessageHandler createMqHandler = new MessageHandler() {  
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			String mqName = msg.getHead("mq_name", "");
			mqName = mqName.trim();
			if("".equals(mqName)){
				msg.setBody("Missing mq_name");
				ReplyKit.reply400(msg, sess);
				return;
			}
			String mqMode = msg.getHead("mq_mode", "");
			mqMode = mqMode.trim();
			if("".equals(mqMode)){
				msg.setBody("Missing mq_mode");
				ReplyKit.reply400(msg, sess);
				return;
			}
			int mode = 0;
    		try{
    			mode = Integer.valueOf(mqMode); 
    		} catch (Exception e){
    			msg.setBody("mqMode invalid");
    			ReplyKit.reply400(msg, sess);
        		return;  
    		}
    		
    		AbstractMQ mq = null;
    		synchronized (mqTable) {
    			mq = mqTable.get(mqName);
    			if(mq != null){
    				ReplyKit.reply200(msg, sess);
    				return;
    			}
    			
    			AbstractQueue<Message> support = null;
				if(MqMode.isEnabled(mode, MqMode.Memory)){
					support = new MessageMemoryQueue();
				} else {
					support = new MessageDiskQueue(mqName, mode);
				}
				
    			if(MqMode.isEnabled(mode, MqMode.PubSub)){ 
    				mq = new PubSub(mqName, support);
    			} else {
    				mq = new MQ(mqName, support);
    			}
    			mq.creator = sess.getRemoteAddress();
    			log.info("MQ Created: %s", mq);
    			mqTable.put(mqName, mq);
    			ReplyKit.reply200(msg, sess);
    		}
		}
	};  
	
	private MessageHandler heartbeatHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			// just ignore
		}
	};
	
	protected void onSessionAccepted(Session sess) throws IOException {
		sessionTable.put(sess.id(), sess);
		super.onSessionAccepted(sess); 
	}

	@Override
	protected void onException(Throwable e, Session sess) throws IOException { 
		sessionTable.remove(sess.id());
		super.onException(e, sess);
	}
	
	@Override
	protected void onSessionDestroyed(Session sess) throws IOException { 
		sessionTable.remove(sess.id());
		super.onSessionDestroyed(sess);
	} 
	
    public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}  
     
    public BrokerInfo getStatInfo(){
    	Map<String, MqInfo> table = new HashMap<String, MqInfo>();
   		for(Map.Entry<String, AbstractMQ> e : this.mqTable.entrySet()){
   			table.put(e.getKey(), e.getValue().getMqInfo());
   		}  
		BrokerInfo info = new BrokerInfo();
		info.broker = serverAddr;
		info.mqTable = table;  
		return info;
    }
    
    public void loadMQ(String storePath){ 
    	log.info("Loading DiskQueues...");
    	mqTable.clear();
		DiskQueuePool.init(storePath); 
		
		Map<String, DiskQueue> dqs = DiskQueuePool.getQueryMap();
		for(Entry<String, DiskQueue> e : dqs.entrySet()){
			AbstractMQ mq;
			String name = e.getKey();
			DiskQueue diskq = e.getValue();
			int flag = diskq.getFlag(); 
			AbstractQueue<Message> queue = new MessageDiskQueue(name, diskq);
			if( MqMode.isEnabled(flag, MqMode.PubSub)){ 
				mq = new PubSub(name, queue);
			}  else {
				mq = new MQ(name, queue); 
			}
			mq.lastUpdateTime = System.currentTimeMillis(); 
			mq.creator = "System";
			mqTable.put(name, mq);
		}
    }   
    
    public void close() throws IOException {   
    	this.cleanExecutor.shutdownNow();
    	DiskQueuePool.destory(); 
    }
    
    
    private class AdminHandler implements MessageHandler {
		private Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<String, MessageHandler>();
		public AdminHandler() {
			handlerMap.put("", homeHandler);
			handlerMap.put("jquery", jqueryHandler);
			handlerMap.put("data", dataHandler); 
		}

		public void handle(Message msg, Session sess) throws IOException {
			String subCmd = msg.getSubCmd(); //from header first
			if (subCmd == null){
				subCmd = msg.getRequestParam(Message.SUB_CMD); //from request Param
			}
			if(subCmd == null){ //default to home
				subCmd = "";
			}
			
			MessageHandler handler = this.handlerMap.get(subCmd);
			if (handler == null) {
				msg.setBody("sub_cmd=%s Not Found", subCmd);
				ReplyKit.reply404(msg, sess);
				return;
			}
			handler.handle(msg, sess);
		}
		
		private MessageHandler homeHandler = new MessageHandler() {
			public void handle(Message msg, Session sess) throws IOException {
				msg = new Message();
				msg.setResponseStatus("200");
				msg.setHead("content-type", "text/html");
				String body = FileKit.loadFileContent("zbus.htm");
				if ("".equals(body)) {
					body = "<strong>zbus.htm file missing</strong>";
				}
				msg.setBody(body);
				sess.write(msg);
			}
		};
		
		private MessageHandler jqueryHandler = new MessageHandler() {
			public void handle(Message msg, Session sess) throws IOException {
				msg = new Message();
				msg.setResponseStatus("200");
				msg.setHead("content-type", "application/javascript");
				String body = FileKit.loadFileContent("jquery.js");
				msg.setBody(body);
				sess.write(msg);
			}
		};
		
		private MessageHandler dataHandler = new MessageHandler() {
			public void handle(Message msg, Session sess) throws IOException {
				BrokerInfo info = getStatInfo();

				Message data = new Message();
				data.setResponseStatus("200");
				data.setId(msg.getId());
				data.setHead("content-type", "application/json");
				data.setBody(JsonKit.toJson(info));
				sess.write(data);
			}
		};
		
	}
    
    
    @SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		MqServerConfig config = new MqServerConfig();
		config.serverHost = ConfigKit.option(args, "-h", "0.0.0.0");
		config.serverPort = ConfigKit.option(args, "-p", 15555);
		config.selectorCount = ConfigKit.option(args, "-selector", 1);
		config.executorCount = ConfigKit.option(args, "-executor", 64);
		config.verbose = ConfigKit.option(args, "-verbose", true);
		config.storePath = ConfigKit.option(args, "-store", "mq");

		String configFile = ConfigKit.option(args, "-conf", "zbus.properties");
		InputStream is = FileKit.loadFile(configFile);
		if(is != null){
			log.info("Using file config options from(%s)", configFile);
			config.load(configFile);
		} else {
			log.info("Using command line config options");
		}
		
		log.info("MqServer starting ...");

		Dispatcher dispatcher = new Dispatcher()
			.selectorCount(config.selectorCount)
			.executorCount(config.executorCount);

		String address = config.serverHost + ":" + config.serverPort; 
		
		final MqAdaptor mqAdaptor = new MqAdaptor(address);
		mqAdaptor.setVerbose(config.verbose); 
		mqAdaptor.loadMQ(config.storePath);
		
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try {
					mqAdaptor.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		});
		
		Server server = new Server(dispatcher, mqAdaptor, address);
		server.setServerName("MqServer");
		server.start();
		
		log.info("MqServer started successfully");
	}
}



