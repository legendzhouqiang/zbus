package org.zbus.mq.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.zbus.kit.FileKit;
import org.zbus.kit.JsonKit;
import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.mq.ConsumeGroup;
import org.zbus.mq.Protocol;
import org.zbus.mq.Protocol.BrokerInfo;
import org.zbus.mq.Protocol.MqInfo;
import org.zbus.mq.disk.DiskMessage;
import org.zbus.net.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageAdaptor;

public class MqAdaptor extends MessageAdaptor implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(MqAdaptor.class);

	private final Map<String, MessageQueue> mqTable; 
	private final Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<String, MessageHandler>();
	
	private boolean verbose = false;    
	private final MqServer mqServer;
	private final MqServerConfig config;  
	
	private ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(16);

 
	public MqAdaptor(MqServer mqServer){
		super(mqServer.getSessionTable());
		
		this.config = mqServer.getConfig();
		
		this.mqServer = mqServer; 
		this.mqTable = mqServer.getMqTable();  
		
		registerHandler(Protocol.Produce, produceHandler); 
		registerHandler(Protocol.Consume, consumeHandler);  
		registerHandler(Protocol.Route, routeHandler); 
		
		registerHandler(Protocol.CreateMQ, createMqHandler);
		registerHandler(Protocol.QueryMQ, queryMqHandler);
		registerHandler(Protocol.RemoveMQ, removeMqHandler); 
		
		registerHandler("", homeHandler);  
		registerHandler(Protocol.Data, dataHandler); 
		registerHandler(Protocol.Jquery, jqueryHandler);
		registerHandler(Protocol.Test, testHandler);
		
		registerHandler(Message.HEARTBEAT, heartbeatHandler);   
		
	}   
	
	private boolean validateMessage(Message msg, Session session) throws IOException{
		final boolean ack = msg.isAck();  
		String id = msg.getId();
		String tag = msg.getTag();
		if(id != null && id.length()>DiskMessage.ID_MAX_LEN){
			msg.setBody("Message.Id length should <= "+DiskMessage.ID_MAX_LEN);
			if(ack) ReplyKit.reply400(msg, session);
			return false;
		}
		if(tag != null && tag.length()>DiskMessage.TAG_MAX_LEN){
			msg.setBody("Message.Tag length should <= "+DiskMessage.TAG_MAX_LEN);
			if(ack) ReplyKit.reply400(msg, session);
			return false;
		}
		return true;
	}
	
	private MessageHandler produceHandler = new MessageHandler() { 
		@Override
		public void handle(final Message msg, final Session sess) throws IOException {  
			boolean ok = validateMessage(msg,sess);
			if(!ok) return;
			
			if(!auth(msg)){ 
				ReplyKit.reply403(msg, sess);
				return;
			}
			
			final MessageQueue mq = findMQ(msg, sess);
			if(mq == null) return; 
			
			if((mq.getFlag()&Protocol.FlagRpc) != 0){ 
				if(mq.consumerCount(null) == 0){ //default consumeGroup
					ReplyKit.reply502(msg, sess);
					return;
				}
			} 
			
			final boolean ack = msg.isAck();  
			msg.removeHead(Protocol.CMD);
			msg.removeHead(Protocol.ACK); 
			Long ttl = msg.getTtl();
			if(ttl != null){
				try{ 
					msg.setExpire(System.currentTimeMillis()+ttl); 
				} catch(IllegalArgumentException e){
					//ignore
				}
			}
			 
			Long delay = msg.getDelay();
			if(delay != null){ 
				if(delay > 0){
					timer.schedule(new Runnable() { 
						@Override
						public void run() {
							try {
								mq.produce(msg, sess); 
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}  
						}
					}, delay, TimeUnit.MILLISECONDS);
					
					if(ack){
						ReplyKit.reply200(msg, sess);
					}
					return;
				} 
			} 
			
			mq.produce(msg, sess);  
			
			if(ack){
				ReplyKit.reply200(msg, sess);
			}
		}
	}; 
	
	private MessageHandler consumeHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			if(!auth(msg)){ 
				ReplyKit.reply403(msg, sess);
				return;
			}
			
			MessageQueue mq = findMQ(msg, sess);
			if(mq == null) return; 
			
			mq.consume(msg, sess);
			
			String mqName = sess.attr("mq");
			if(!msg.getMq().equals(mqName)){
				sess.attr("mq", mq.getName()); //mark
				mqServer.pubEntryUpdate(mq); //notify TrackServer
			} 
		}
	}; 
	
	private MessageHandler routeHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			String recver = msg.getRecver();
			if(recver == null) {
				return; //just ignore
			}
			Session target = sessionTable.get(recver);
			if(target == null) {
				log.warn("Missing target %s", recver); 
				return; //just ignore
			} 
			msg.removeHead(Protocol.ACK);
			msg.removeHead(Protocol.RECVER);
			msg.removeHead(Protocol.CMD);
			
			String status = "200";
			if(msg.getOriginStatus() != null){
				status = msg.getOriginStatus(); 
				msg.removeHead(Protocol.ORIGIN_STATUS);
			} 
			msg.setStatus(status);
			
			try{
				target.write(msg);
			} catch(Exception ex){
				log.warn("Target(%s) write failed, Ignore", recver); 
				return; //just ignore
			}
		}
	}; 
	
	private MessageHandler removeMqHandler = new MessageHandler() {  
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			String registerToken = msg.getHead("register_token", "");
			if(!registerToken.equals(config.getRegisterToken())){
				msg.setBody("registerToken unmatched");
				ReplyKit.reply403(msg, sess);
				return; 
			}
			String mqName = msg.getHead("mq_name", "");
			mqName = mqName.trim();
			if("".equals(mqName)){
				msg.setBody("Missing mq_name");
				ReplyKit.reply400(msg, sess);
				return;
			}
			synchronized (mqTable) {
				MessageQueue mq = mqTable.get(mqName);
    			if(mq == null){ 
    				ReplyKit.reply404(msg, sess);
    				return;
    			}   
    			//Clear mapped mq
    			//TODO 
    			
    			mqTable.remove(mqName);  
    			ReplyKit.reply200(msg, sess);
			}
		}
	};
	
	private MessageHandler createMqHandler = new MessageHandler() {  
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			if(!auth(msg)){ 
				ReplyKit.reply403(msg, sess);
				return;
			}
    		
			String mqName = msg.getMq();
			if(mqName == null){ //backward compatible, to remove
				mqName = msg.getHead("mq_name", "");
			} 
			mqName = mqName.trim();
			if("".equals(mqName)){
				msg.setBody("Missing mq_name");
				ReplyKit.reply400(msg, sess);
				return;
			}
			Integer flag = msg.getFlag();   
    		ConsumeGroup ctrl = new ConsumeGroup(msg);  
    		MessageQueue mq = null;
    		synchronized (mqTable) {
    			mq = mqTable.get(mqName); 
    			boolean newMq = false;
    			if(mq == null){
    				newMq = true;
					File mqFile = new File(config.storePath, mqName);
	    			mq = new DiskQueue(mqFile); 
	    			if(flag != null){
	    				mq.setFlag(flag);
	    			}
	    			mq.setCreator(sess.getRemoteAddress()); 
	    			mqTable.put(mqName, mq);
    			}
    			try {
					mq.declareConsumeGroup(ctrl);
					mqServer.pubEntryUpdate(mq);
					
					ReplyKit.reply200(msg, sess);
					if(newMq){
						log.info("MQ Created: %s", mq);
					}   
					log.info("MQ Declared: %s", ctrl); 
				} catch (Exception e) { 
					log.error(e.getMessage(), e);
					ReplyKit.reply500(msg, e, sess);
				} 
    		}
		}
	};   
	
	private MessageHandler testHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = new Message();
			res.setStatus(200); 
			res.setId(msg.getId()); 
			res.setBody("OK");
			sess.write(res);
		}
	};
	
	private MessageHandler homeHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			String msgId = msg.getId();
			msg = new Message();
			msg.setStatus("200");
			msg.setId(msgId);
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
			msg.setStatus("200");
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
			data.setStatus("200");
			data.setId(msg.getId());
			data.setHead("content-type", "application/json");
			data.setBody(JsonKit.toJson(info));
			sess.write(data);
		}
	};
	
	private MessageHandler queryMqHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			String json = "";
			if(msg.getMq() == null){
				BrokerInfo info = getStatInfo();
				json = JsonKit.toJson(info);
			} else { 
				MessageQueue mq = findMQ(msg, sess);
		    	if(mq == null){ 
					return;
				} else {
					json = JsonKit.toJson(mq.getMqInfo());
				}
			}

			Message data = new Message();
			data.setStatus("200");
			data.setId(msg.getId());
			data.setHead("content-type", "application/json");
			data.setBody(json);
			sess.write(data);
		}
	}; 
	
	private MessageHandler heartbeatHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			// just ignore
		}
	};
	
	protected void cleanSession(Session sess) throws IOException{
		super.cleanSession(sess);
		
		String mqName = sess.attr("mq");
		if(mqName == null) return;
		
		MessageQueue mq = mqTable.get(mqName); 
		if(mq == null) return; 
		mq.cleanSession(sess);
		
		mqServer.pubEntryUpdate(mq); 
	} 
	
	private boolean auth(Message msg){ 
		//String appid = msg.getAppid();
		//String token = msg.getToken(); 
		//TODO add authentication
		return true;
	}
	
    public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}  
     
    public BrokerInfo getStatInfo(){
    	Map<String, MqInfo> table = new HashMap<String, MqInfo>();
   		for(Map.Entry<String, MessageQueue> e : this.mqTable.entrySet()){
   			MqInfo info = e.getValue().getMqInfo();
   			info.consumerInfoList.clear(); //clear to avoid long list
   			table.put(e.getKey(), info);
   		}  
		BrokerInfo info = new BrokerInfo();
		info.broker = mqServer.getServerAddr();
		info.mqTable = table;  
		return info;
    }
    
	public void loadMQ() throws IOException {
		log.info("Loading DiskQueues...");
		mqTable.clear();
		
		File[] mqDirs = new File(config.storePath).listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		if (mqDirs != null && mqDirs.length > 0) {
			for (File mqDir : mqDirs) {
				MessageQueue mq = new DiskQueue(mqDir);
				mqTable.put(mqDir.getName(), mq);
				log.info("MQ=%s loaded", mqDir.getName());
				
				mqServer.pubEntryUpdate(mq);
			}
		} 
	}
    
    public void close() throws IOException {     
    	if(this.timer != null){
    		this.timer.shutdown();
    	}
    } 
    
    private void handleUrlMessage(Message msg){ 
    	if(msg.getCmd() != null){
    		return;
    	} 
    	String url = msg.getUrl(); 
    	if(url == null || "/".equals(url)){
    		msg.setCmd("");
    		return;
    	} 
    	int idx = url.indexOf('?');
    	String cmd = "";
    	if(idx >= 0){
    		cmd = url.substring(1, idx); 
    		if(url.charAt(idx-1) == '/'){
    			cmd = url.substring(1, idx-1);
    		} else {
    			cmd = url.substring(1, idx);
    		}
    	} else {
    		cmd = url.substring(1);
    	}  
    	
    	msg.setCmd(cmd);
    	msg.urlToHead(); 
	}
    
    public void onSessionMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	msg.setSender(sess.id());
		msg.setServer(mqServer.getServerAddr()); 
		msg.setRemoteAddr(sess.getRemoteAddress());
		
		if(verbose){
			log.info("\n%s", msg);
		}
		
		handleUrlMessage(msg);
		
		String cmd = msg.getCmd(); 
    	if(cmd != null){
	    	MessageHandler handler = handlerMap.get(cmd);
	    	if(handler != null){
	    		handler.handle(msg, sess);
	    		return;
	    	}
    	}
    	
    	Message res = new Message();
    	res.setId(msg.getId()); 
    	res.setStatus(400);
    	String text = String.format("Bad format: command(%s) not support", cmd);
    	res.setBody(text); 
    	sess.write(res); 
    }  
	
    private MessageQueue findMQ(Message msg, Session sess) throws IOException{
		String mqName = msg.getMq();
		MessageQueue mq = mqTable.get(mqName); 
    	if(mq == null){
    		ReplyKit.reply404(msg, sess); 
    		return null;
    	} 
    	return mq;
	}
     
    public void registerHandler(String command, MessageHandler handler){
    	this.handlerMap.put(command, handler);
    } 
}