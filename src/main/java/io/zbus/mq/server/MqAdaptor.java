package io.zbus.mq.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Message;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.BrokerInfo;
import io.zbus.mq.Protocol.MqInfo;
import io.zbus.mq.disk.DiskMessage;
import io.zbus.mq.net.MessageAdaptor;
import io.zbus.mq.net.MessageHandler;
import io.zbus.net.Session;
import io.zbus.util.FileUtil;
import io.zbus.util.JsonUtil;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

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
		
		registerHandler(Protocol.DeclareTopic, declareTopicHandler); 
		registerHandler(Protocol.QueryTopic, queryTopicHandler);
		registerHandler(Protocol.RemoveTopic, removeTopicHandler); 
		
		registerHandler("", homeHandler);  
		registerHandler(Protocol.Data, dataHandler); 
		registerHandler(Protocol.Jquery, jqueryHandler);
		registerHandler(Protocol.Ping, pingHandler);
		
		registerHandler(Message.HEARTBEAT, heartbeatHandler);   
		
		//TODO backward compatible
		registerHandler("create_mq", declareTopicHandler);
		registerHandler("query_mq", queryTopicHandler);
		registerHandler("remove_mq", removeTopicHandler);
		
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
			msg.removeHead(Protocol.COMMAND);
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
			
			String topic = sess.attr(Protocol.Topic);
			if(!msg.getTopic().equals(topic)){
				sess.attr(Protocol.Topic, mq.getName()); //mark
				mqServer.pubEntryUpdate(mq); //notify TrackServer
			} 
		}
	}; 
	
	private MessageHandler routeHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			String recver = msg.getReceiver();
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
			msg.removeHead(Protocol.COMMAND);
			
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
	
	private MessageHandler removeTopicHandler = new MessageHandler() {  
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
	
	private MessageHandler declareTopicHandler = new MessageHandler() {  
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			if(!auth(msg)){ 
				ReplyKit.reply403(msg, sess);
				return;
			}
    		
			String topic = msg.getTopic();
			if(topic == null){ //TODO backward compatible
				topic = msg.getHead("mq");
				if(topic == null){
					topic = msg.getHead("mq_name", "");
				}
			} 
			topic = topic.trim();
			if("".equals(topic)){
				msg.setBody("Missing topic(alias: mq)");
				ReplyKit.reply400(msg, sess);
				return;
			}
			Integer flag = msg.getFlag();   
    		ConsumeGroup ctrl = new ConsumeGroup(msg);  
    		MessageQueue mq = null;
    		synchronized (mqTable) {
    			mq = mqTable.get(topic); 
    			boolean newMq = false;
    			if(mq == null){
    				newMq = true;
					File mqFile = new File(config.storePath, topic);
	    			mq = new DiskQueue(mqFile); 
	    			if(flag != null){
	    				mq.setFlag(flag);
	    			}
	    			mq.setCreator(sess.getRemoteAddress()); 
	    			mqTable.put(topic, mq);
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
	
	private MessageHandler pingHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = new Message();
			res.setStatus(200); 
			res.setId(msg.getId()); 
			res.setBody(""+System.currentTimeMillis());
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
			String body = FileUtil.loadFileContent("zbus.htm");
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
			String body = FileUtil.loadFileContent("jquery.js");
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
			data.setBody(JsonUtil.toJson(info));
			sess.write(data);
		}
	};
	
	private MessageHandler queryTopicHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			String json = "";
			if(msg.getTopic() == null){
				BrokerInfo info = getStatInfo();
				json = JsonUtil.toJson(info);
			} else { 
				MessageQueue mq = findMQ(msg, sess);
		    	if(mq == null){ 
					return;
				} else {
					json = JsonUtil.toJson(mq.getMqInfo());
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
		
		String topic = sess.attr(Protocol.Topic);
		if(topic == null) return;
		
		MessageQueue mq = mqTable.get(topic); 
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
    	if(msg.getCommand() != null){
    		return;
    	} 
    	String url = msg.getUrl(); 
    	if(url == null || "/".equals(url)){
    		msg.setCommand("");
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
    	
    	msg.setCommand(cmd);
    	msg.urlToHead(); 
	}
    
    public void sessionMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	msg.setSender(sess.id());
		msg.setServer(mqServer.getServerAddr()); 
		msg.setRemoteAddr(sess.getRemoteAddress());
		
		if(verbose){
			log.info("\n%s", msg);
		}
		
		handleUrlMessage(msg);
		
		String cmd = msg.getCommand(); 
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
		String topic = msg.getTopic();
		if(topic == null){ 
			topic = msg.getHead("mq"); //TODO backward compatible
		}
		MessageQueue mq = mqTable.get(topic); 
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