package io.zbus.mq.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.zbus.mq.ConsumerGroup;
import io.zbus.mq.Message;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ServerEvent;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.disk.DiskMessage;
import io.zbus.mq.net.MessageAdaptor;
import io.zbus.mq.net.MessageHandler;
import io.zbus.net.Session;
import io.zbus.rpc.Request;
import io.zbus.util.FileUtil;
import io.zbus.util.JsonUtil;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;

public class MqAdaptor extends MessageAdaptor implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(MqAdaptor.class);

	private final Map<String, MessageQueue> mqTable; 
	private final Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<String, MessageHandler>();
	
	private boolean verbose = false;    
	private final MqServer mqServer;
	private final MqServerConfig config;    
	private final Tracker trackService;
	private final TraceService traceService;
	
	private ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(16);

 
	public MqAdaptor(MqServer mqServer){
		super(mqServer.getSessionTable());
		
		this.config = mqServer.getConfig();
		
		this.mqServer = mqServer; 
		this.mqTable = mqServer.getMqTable();  
		this.trackService = mqServer.getTracker();
		this.traceService = mqServer.getTraceService();
		
		
		//Produce/Consume
		registerHandler(Protocol.PRODUCE, produceHandler); 
		registerHandler(Protocol.CONSUME, consumeHandler);  
		registerHandler(Protocol.ROUTE, routeHandler); 
		registerHandler(Protocol.RPC, rpcHandler);
		
		//Topic/ConsumerGroup 
		registerHandler(Protocol.DECLARE_TOPIC, declareTopicHandler); 
		registerHandler(Protocol.QUERY_TOPIC, queryTopicHandler);
		registerHandler(Protocol.REMOVE_TOPIC, removeTopicHandler); 
		
		//Tracker
		registerHandler(Protocol.TRACK_QUERY, trackQueryHandler); 
		registerHandler(Protocol.TRACK_PUB, trackPubServerHandler); 
		registerHandler(Protocol.TRACK_SUB, trackSubHandler); 
		
		
		//Monitor/Management
		registerHandler("", homeHandler);  
		registerHandler(Protocol.JS, jsHandler); 
		registerHandler(Protocol.CSS, cssHandler);
		registerHandler(Protocol.IMG, imgHandler);
		registerHandler("favicon.ico", faviconHandler);
		registerHandler(Protocol.PAGE, pageHandler);
		registerHandler(Protocol.PING, pingHandler);
		registerHandler(Protocol.INFO, infoHandler);
		registerHandler(Protocol.VERSION, versionHandler);
		registerHandler(Protocol.TRACE, traceHandler);
		
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
			
			if((mq.getFlag()&Protocol.FLAG_RPC) != 0){ 
				if(mq.consumerCount(null) == 0){ //default consumerGroup
					ReplyKit.reply502(msg, sess);
					return;
				}
			} 
			
			final boolean ack = msg.isAck();  
			msg.removeHeader(Protocol.COMMAND);
			msg.removeHeader(Protocol.ACK); 
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
	
	private MessageHandler rpcHandler = new MessageHandler() { 
		@Override
		public void handle(final Message msg, final Session sess) throws IOException {  
			msg.setAck(false);
			produceHandler.handle(msg, sess);
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
			
			String topic = sess.attr(Protocol.TOPIC);
			if(!msg.getTopic().equals(topic)){
				sess.attr(Protocol.TOPIC, mq.getName()); //mark
				
				trackService.publish(); 
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
			msg.removeHeader(Protocol.ACK);
			msg.removeHeader(Protocol.RECVER);
			msg.removeHeader(Protocol.COMMAND);
			
			String status = "200";
			if(msg.getOriginStatus() != null){
				status = msg.getOriginStatus(); 
				msg.removeHeader(Protocol.ORIGIN_STATUS);
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
			if(!auth(msg)){ 
				ReplyKit.reply403(msg, sess);
				return;
			}
			String mqName = msg.getHeader("mq_name", "");
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
				topic = msg.getHeader("mq");
				if(topic == null){
					topic = msg.getHeader("mq_name", "");
				}
			} 
			topic = topic.trim();
			if("".equals(topic)){
				msg.setBody("Missing topic(alias: mq)");
				ReplyKit.reply400(msg, sess);
				return;
			}
			Integer flag = msg.getFlag();   
    		ConsumerGroup ctrl = new ConsumerGroup(msg);  
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
					mq.declareConsumerGroup(ctrl); 
					trackService.publish(); 
					
					ReplyKit.reply200(msg, sess);
					if(newMq){
						log.info("MQ Created: %s", mq);
					}   
					log.info("MQ Declared: %s", ctrl); 
				} catch (Exception e) { 
					log.error(e.getMessage(), e);
					ReplyKit.reply500(msg, e, sess); //TODO client update to handle 500
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
	
	private MessageHandler infoHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = new Message();
			res.setStatus(200); 
			res.setId(msg.getId()); 
			res.setJsonBody(JsonUtil.toJSONString(getServerInfo()));  
			sess.write(res);
		}
	};
	
	private MessageHandler versionHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = new Message();
			res.setStatus(200); 
			res.setId(msg.getId()); 
			res.setBody(Protocol.VERSION_VALUE);
			sess.write(res);
		}
	};
	
	private MessageHandler homeHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			String msgId = msg.getId();
			msg = new Message();
			msg.setStatus("200");
			msg.setId(msgId);
			msg.setHeader("content-type", "text/html");
			String body = FileUtil.loadFileString("home.htm");
			if ("".equals(body)) {
				body = "<strong>zbus.htm file missing</strong>";
			}
			msg.setBody(body);
			sess.write(msg);
		}
	};  
	

	private Message handleTemplateRequest(String prefixPath, String url){
		Message res = new Message(); 
		if(!url.startsWith(prefixPath)){
			res.setStatus(400);
			res.setBody("Missing file name in URL"); 
			return res;
		}
		url = url.substring(prefixPath.length());   
		int idx = url.lastIndexOf('?');
		Map<String, Object> model = null;
		String fileName = url;
		if(idx >= 0){
			fileName = url.substring(0, idx); 
			String params = url.substring(idx+1);
			model = FileUtil.parseKeyValuePairs(params);
		}
		String body = null;
		try{
			body = FileUtil.loadTemplate(fileName, model);
			if(body == null){
				res.setStatus(404);
				body = "404: File (" + fileName +") Not Found";
			} else {
				res.setStatus(200); 
			}
		} catch (IOException e){
			res.setStatus(404);
			body = e.getMessage();
		}  
		res.setBody(body); 
		return res;
	}
	
	private Message handleFileRequest(String prefixPath, String url){
		Message res = new Message(); 
		if(!url.startsWith(prefixPath)){
			res.setStatus(400);
			res.setBody("Missing file name in URL"); 
			return res;
		}
		url = url.substring(prefixPath.length());   
		int idx = url.lastIndexOf('?');
		String fileName = url;
		if(idx >= 0){
			fileName = url.substring(0, idx); 
		}
		byte[] body = null;
		try{
			body = FileUtil.loadFileBytes(fileName);
			if(body == null){
				res.setStatus(404);
				body = ("404: File (" + fileName +") Not Found").getBytes();
			} else {
				res.setStatus(200); 
			}
		} catch (IOException e){
			res.setStatus(404);
			body = e.getMessage().getBytes();
		}  
		res.setBody(body); 
		return res;
	}
	
	private MessageHandler pageHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException { 
			Message res = handleTemplateRequest("/page/", msg.getUrl());
			if("200".equals(res.getStatus())){
				res.setHeader("content-type", "text/html");
			}
			sess.write(res); 
		}
	};
	
	private MessageHandler jsHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/js/", msg.getUrl());
			if("200".equals(res.getStatus())){
				res.setHeader("content-type", "application/javascript");
			}
			sess.write(res); 
		}
	};
	
	private MessageHandler cssHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/css/", msg.getUrl());
			if("200".equals(res.getStatus())){
				res.setHeader("content-type", "text/css");
			} 
			sess.write(res);
		}
	}; 
	
	private MessageHandler imgHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/img/", msg.getUrl());
			if("200".equals(res.getStatus())){
				res.setHeader("content-type", "image/svg+xml");
			} 
			sess.write(res);
		}
	}; 
	
	private MessageHandler faviconHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/img/", "/img/logo.svg");
			if("200".equals(res.getStatus())){
				res.setHeader("content-type", "image/svg+xml");
			} 
			sess.write(res);
		}
	}; 
	
	private MessageHandler queryTopicHandler = new MessageHandler() {
		public void handle(Message msg, Session sess) throws IOException {
			String json = "";
			if(msg.getTopic() == null){
				ServerInfo info = getServerInfo();
				json = JsonUtil.toJSONString(info.topicMap);
			} else { 
				MessageQueue mq = findMQ(msg, sess);
		    	if(mq == null){ 
					return;
				} else {
					json = JsonUtil.toJSONString(mq.getTopicInfo());
				}
			}

			Message data = new Message();
			data.setStatus("200");
			data.setId(msg.getId());
			data.setHeader("content-type", "application/json");
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
	
	private MessageHandler trackQueryHandler = new MessageHandler() {
		
		@Override
		public void handle(Message msg, Session session) throws IOException {  
			Message res = new Message();
			res.setStatus(200);
			res.setJsonBody(JsonUtil.toJSONString(trackService.queryTrackerInfo()));
			ReplyKit.reply(msg, res, session);
		}
	};
	
	private MessageHandler trackPubServerHandler = new MessageHandler() {
		
		@Override
		public void handle(Message msg, Session session) throws IOException {  
			try{
				boolean ack = msg.isAck();
				ServerEvent event = JsonUtil.parseObject(msg.getBodyString(), ServerEvent.class);   
				trackService.publish(event); 
				
				if(ack){
					ReplyKit.reply200(msg, session);
				}
			} catch (Exception e) { 
				ReplyKit.reply500(msg, e, session);
			} 
		}
	};
	 
	
	private MessageHandler trackSubHandler = new MessageHandler() {
		
		@Override
		public void handle(Message msg, Session session) throws IOException { 
			trackService.subscribe(msg, session);
		}
	}; 
	
	private MessageHandler traceHandler = new MessageHandler() {
		
		@Override
		public void handle(Message msg, Session session) throws IOException { 
			traceService.subscribe(msg, session); 
		}
	}; 
	
	protected void cleanSession(Session sess) throws IOException{
		super.cleanSession(sess);
		
		String topic = sess.attr(Protocol.TOPIC);
		if(topic != null){
			MessageQueue mq = mqTable.get(topic); 
			if(mq != null){
				mq.cleanSession(sess); 
				trackService.publish();
			}
		}
		
		trackService.cleanSession(sess); 
		traceService.cleanSession(sess);
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
     
    public ServerInfo getServerInfo(){
    	String serverAddress = mqServer.getServerAddress();
    	Map<String, TopicInfo> table = new HashMap<String, TopicInfo>();
   		for(Map.Entry<String, MessageQueue> e : this.mqTable.entrySet()){
   			TopicInfo info = e.getValue().getTopicInfo(); 
   			info.serverAddress = serverAddress;
   			table.put(e.getKey(), info);
   		}  
   		ServerInfo info = new ServerInfo();
		info.serverAddress = mqServer.getServerAddress();
		info.topicMap = table;  
		
		String serverList = config.getTrackServerList();
		if(serverList == null){
			serverList = "";
		}
		serverList = serverList.trim();
		
		info.trackerList = new ArrayList<String>();
		for(String s : serverList.split("[;, ]")){
			s = s.trim();
			if("".equals(s)) continue;
			info.trackerList.add(s);
		}
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
				log.info("Topic = %s loaded", mqDir.getName()); 
			}
		}   
	}
    
    public void close() throws IOException {     
    	if(this.timer != null){
    		this.timer.shutdown();
    	}
    } 
    
    private void handlUrlRpcMessage(Message msg){
    	// rpc/<topic>/<method>/<param_1>/../<param_n>[?module=<module>&&<header_ext_kvs>]
    	String url = msg.getUrl(); 
    	int idx = url.indexOf('?');
    	String rest = "";
    	Map<String, String> kvs = null;
    	if(idx >= 0){
    		kvs = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    		rest = url.substring(1, idx);  
    		String paramString = url.substring(idx+1); 
    		StringTokenizer st = new StringTokenizer(paramString, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) {
                	String key = e.substring(0, sep).trim().toLowerCase();
                	String val = e.substring(sep + 1).trim();  
                	kvs.put(key, val); 
                }  
            }  
    	} else {
    		rest = url.substring(1);
    	}  
    	
    	String[] bb = rest.split("/");
    	if(bb.length < 3){
    		//ignore invalid 
    		return;
    	}
    	
    	String topic = bb[1];
    	String method = bb[2];
    	msg.setTopic(topic);
    	Request req = new Request();
    	req.setMethod(method); 
    	if(kvs != null && kvs.containsKey("module")){
    		req.setModule(kvs.get("module"));
    	}
    	if(bb.length>3){
    		Object[] params = new Object[bb.length-3];
    		for(int i=0;i<params.length;i++){
    			params[i] = bb[3+i];
    		}
    		req.setParams(params); 
    	} 
    	
    	msg.setBody(JsonUtil.toJSONString(req));
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
    	} else {
    		cmd = url.substring(1);
    	} 
    	idx = cmd.indexOf('/');
    	if(idx > 0){
    		cmd = cmd.substring(0, idx);
    	}
    	
    	msg.setCommand(cmd.toLowerCase());
    	//handle RPC
    	if(Protocol.RPC.equalsIgnoreCase(cmd) && msg.getBody() == null){ 
    		handlUrlRpcMessage(msg); 
    	} else {
    		msg.urlToHead(); 
    	}
	}
    
    private boolean ignoreOnTrace(String cmd){
    	if(Protocol.TRACE.equals(cmd)) return true;
    	if(Message.HEARTBEAT.equals(cmd)) return true;
    	return false;
    }
    
    public void sessionMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	msg.setSender(sess.id());
		msg.setServer(mqServer.getServerAddress()); 
		msg.setRemoteAddr(sess.getRemoteAddress());
		if(msg.getId() == null){
			msg.setId(UUID.randomUUID().toString());
		}
		
		if(verbose){
			log.info("\n%s", msg);
		} 
		
		
		handleUrlMessage(msg); 
		
		String cmd = msg.getCommand(); 
		
		if(!ignoreOnTrace(cmd)){
			try {
				traceService.publish(msg);
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		} 
		
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
			topic = msg.getHeader("mq"); //TODO backward compatible
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