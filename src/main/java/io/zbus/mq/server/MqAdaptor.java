package io.zbus.mq.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.zbus.kit.FileKit;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.DiskQueue;
import io.zbus.mq.MemoryQueue;
import io.zbus.mq.Message;
import io.zbus.mq.MessageQueue;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.ServerEvent;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.disk.DiskMessage;
import io.zbus.mq.server.auth.AuthProvider;
import io.zbus.mq.server.auth.Token;
import io.zbus.rpc.Request;
import io.zbus.transport.MessageHandler;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session; 

public class MqAdaptor extends ServerAdaptor implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(MqAdaptor.class);

	private final Map<String, MessageQueue> mqTable; 
	private final Map<String, MessageHandler<Message>> handlerMap = new ConcurrentHashMap<String, MessageHandler<Message>>();
	private boolean verbose = false;    
	private final MqServer mqServer;
	private final MqServerConfig config;    
	private final Tracker tracker; 
	private AuthProvider authProvider;
	
	private ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(16);
	private Set<String> restUrlCommands = new HashSet<String>(); 
 
	public MqAdaptor(MqServer mqServer){
		super(mqServer.getSessionTable());
		
		this.config = mqServer.getConfig();
		this.authProvider = this.config.getAuthProvider();
		
		this.mqServer = mqServer;  
		this.mqTable = mqServer.getMqTable();  
		this.tracker = mqServer.getTracker(); 
		
		restUrlCommands.add(Protocol.PRODUCE);
		restUrlCommands.add(Protocol.CONSUME);
		restUrlCommands.add(Protocol.DECLARE);
		restUrlCommands.add(Protocol.QUERY);
		restUrlCommands.add(Protocol.REMOVE); 
		restUrlCommands.add(Protocol.EMPTY);
		
		
		//Produce/Consume
		registerHandler(Protocol.PRODUCE, produceHandler); 
		registerHandler(Protocol.CONSUME, consumeHandler);  
		registerHandler(Protocol.ROUTE, routeHandler); 
		registerHandler(Protocol.RPC, rpcHandler);
		registerHandler(Protocol.UNCONSUME, unconsumeHandler); 
		
		//Topic/ConsumerGroup 
		registerHandler(Protocol.DECLARE, declareHandler);  
		registerHandler(Protocol.QUERY, queryHandler);
		registerHandler(Protocol.REMOVE, removeHandler); 
		
		//Tracker  
		registerHandler(Protocol.TRACK_PUB, trackPubServerHandler); 
		registerHandler(Protocol.TRACK_SUB, trackSubHandler); 
		registerHandler(Protocol.TRACKER, trackerHandler); 
		
		registerHandler(Protocol.SERVER, serverHandler); 
		
		registerHandler(Protocol.SSL, sslHandler); 
		
		
		//Monitor/Management
		registerHandler(Protocol.HOME, homeHandler);  
		registerHandler("favicon.ico", faviconHandler);
		
		registerHandler(Protocol.LOGIN, loginHandler);  
		registerHandler(Protocol.LOGOUT, logoutHandler);  
		registerHandler(Protocol.JS, jsHandler); 
		registerHandler(Protocol.CSS, cssHandler);
		registerHandler(Protocol.IMG, imgHandler); 
		registerHandler(Protocol.PAGE, pageHandler);
		registerHandler(Protocol.PING, pingHandler);   
		
		registerHandler(Message.HEARTBEAT, heartbeatHandler);    
		
		
		if(Fix.Enabled){
			//Compatible to older zbus
			registerHandler(Fix.CreateMQ, declareHandler); 
			registerHandler(Fix.QueryMQ, queryHandler); 
			registerHandler(Fix.RemoveMQ, removeHandler); 
		} 
	}   
	
	private MessageHandler<Message> produceHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(final Message msg, final Session sess) throws IOException {  
			boolean ok = validateMessage(msg,sess);
			if(!ok) return; 
			
			final MessageQueue mq = findMQ(msg, sess);
			if(mq == null) return; 
			
			
			final boolean ack = msg.isAck();  
			msg.removeHeader(Protocol.COMMAND);
			msg.removeHeader(Protocol.ACK);  
			msg.removeHeader(Protocol.TOKEN);
			mq.produce(msg);  
			
			if(ack){
				ReplyKit.reply200(msg, sess);
			}
		}
	}; 
	
	private MessageHandler<Message> rpcHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(final Message msg, final Session sess) throws IOException {  
			msg.setAck(false);
			produceHandler.handle(msg, sess);
		}
	};
	
	private MessageHandler<Message> consumeHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException {  
			MessageQueue mq = findMQ(msg, sess);
			if(mq == null) return; 
			
			mq.consume(msg, sess);  
			String topic = sess.attr(Protocol.TOPIC);
			if(!msg.getTopic().equalsIgnoreCase(topic)){
				sess.attr(Protocol.TOPIC, mq.topic()); //mark
				
				tracker.myServerChanged(); 
			} 
		}
	}; 
	
	private MessageHandler<Message> unconsumeHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException {  
			MessageQueue mq = findMQ(msg, sess);
			if(mq == null) return; 
			
			mq.unconsume(msg, sess);  
			String topic = sess.attr(Protocol.TOPIC);
			if(msg.getTopic().equalsIgnoreCase(topic)){ 
				tracker.myServerChanged(); 
			} 
		}
	}; 
	
	private MessageHandler<Message> routeHandler = new MessageHandler<Message>() { 
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
			
			Integer status = 200;
			if(msg.getOriginStatus() != null){
				status = msg.getOriginStatus(); 
				msg.removeHeader(Protocol.ORIGIN_STATUS);
			} 
			msg.setStatus(status);
			
			try{
				target.write(msg);
			} catch(Exception ex){
				log.warn("Target(%s) write failed, Ignore", recver); 
				return; 
			}
		}
	};  
	
	private MessageHandler<Message> declareHandler = new MessageHandler<Message>() {  
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			String topic = msg.getTopic();    
			
			if(StrKit.isEmpty(topic)){ 
				ReplyKit.reply400(msg, sess, "Missing topic");
				return;
			}
			topic = topic.trim();  
			Integer topicMask = msg.getTopicMask();  
    		MessageQueue mq = null;
    		synchronized (mqTable) {
    			mq = mqTable.get(topic);  
    			if(mq == null){ 
    				if(topicMask != null && (topicMask&Protocol.MASK_MEMORY) != 0){
    					mq = new MemoryQueue(topic);
    				} else {
    					mq = new DiskQueue(new File(config.getMqPath(), topic));  
    				} 
	    			mq.setCreator(msg.getToken()); 
	    			mqTable.put(topic, mq);
	    			log.info("MQ Created: %s", mq);
    			}
    		} 
			
			try { 
				if(topicMask != null){
					mq.setMask(topicMask);
				}
				
				String groupName = msg.getConsumeGroup();  
				if(groupName != null){   
					ConsumeGroup consumeGroup = new ConsumeGroup(msg);  
					ConsumeGroupInfo info = mq.declareGroup(consumeGroup); 
					ReplyKit.replyJson(msg, sess, info); 
				} else { 
					if(mq.groupInfo(topic) == null){
						ConsumeGroup consumeGroup = new ConsumeGroup(msg);  
						mq.declareGroup(consumeGroup); 
					}
					TopicInfo topicInfo = mq.topicInfo();
			    	topicInfo.serverAddress = mqServer.getServerAddress();  
					ReplyKit.replyJson(msg, sess, topicInfo);
				}  
				
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
				ReplyKit.reply500(msg, sess, e); 
			} 
			
			tracker.myServerChanged();  
		}
	};
	
	private MessageHandler<Message> queryHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException { 
			Token token = authProvider.getToken(msg.getToken());
			if(msg.getTopic() == null){   
				ServerInfo info = mqServer.serverInfo();
				info = Token.filter(info, token);
				if(info == null){
					ReplyKit.reply404(msg, sess);
				} else {
					ReplyKit.replyJson(msg, sess, tracker.serverInfo(token));
				}
				
				return;
			} 
			
			MessageQueue mq = findMQ(msg, sess);
	    	if(mq == null){ 
	    		ReplyKit.reply404(msg, sess);
				return;
			}
	    	TopicInfo topicInfo = mq.topicInfo();
	    	topicInfo.serverAddress = mqServer.getServerAddress(); 
	    	
			String group = msg.getConsumeGroup();
			if(group == null){
				topicInfo = Token.filter(topicInfo, token);
				if(topicInfo == null){
					ReplyKit.reply404(msg, sess);
				} else {
					ReplyKit.replyJson(msg, sess, topicInfo);
				}
				return;
			}
	    	
			ConsumeGroupInfo groupInfo = topicInfo.consumeGroup(group); 
	    	if(groupInfo == null){
	    		String hint = String.format("404: ConsumeGroup(%s) Not Found", group);
	    		ReplyKit.reply404(msg, sess, hint);
	    		return;
	    	}
	    	ReplyKit.replyJson(msg, sess, groupInfo);  
		}  
	}; 
	
	private MessageHandler<Message> removeHandler = new MessageHandler<Message>() {  
		@Override
		public void handle(Message msg, Session sess) throws IOException {  
			String topic = msg.getTopic(); 
			if(StrKit.isEmpty(topic)){ 
				ReplyKit.reply400(msg, sess, "Missing topic");
				return;
			} 
			topic = topic.trim();   
			MessageQueue mq = mqTable.get(topic);
			if(mq == null){ 
				ReplyKit.reply404(msg, sess);
				return;
			}   
			
			String groupName = msg.getConsumeGroup();
			if(groupName != null){
				try {
					mq.removeGroup(groupName);
					tracker.myServerChanged(); 
					ReplyKit.reply200(msg, sess); 
				} catch (FileNotFoundException e){
					ReplyKit.reply404(msg, sess, "ConsumeGroup("+groupName + ") Not Found"); 
				}  
				return;
			}  
			
			mq = mqTable.remove(mq.topic());
			if(mq != null){
				mq.destroy();
				tracker.myServerChanged(); 
				ReplyKit.reply200(msg, sess);
			} else {
				ReplyKit.reply404(msg, sess, "Topic(" + msg.getTopic() + ") Not Found");
			}
		} 
	};
	
	private MessageHandler<Message> pingHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = new Message();
			res.setStatus(200); 
			res.setId(msg.getId()); 
			res.setBody(""+System.currentTimeMillis());
			sess.write(res);
		}
	};
	 	 
	
	private MessageHandler<Message> homeHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			String tokenStr = msg.getToken();
			Token token = authProvider.getToken(tokenStr);
			Map<String, Object> model = new HashMap<String, Object>();
			String tokenShow = null;
			if(token != null && tokenStr != null){
				tokenShow = String.format("<li><a href='/logout'>%s Logout</a></li>", token.name);
			}
			model.put("token", tokenShow);
			
			ReplyKit.replyTemplate(msg, sess, "home.htm", model);
		}
	};  
	
	private MessageHandler<Message> loginHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			if("GET".equals(msg.getMethod())){
				ReplyKit.replyTemplate(msg, sess, "login.htm"); 
				return;
			} 
			
			Map<String, String> data = StrKit.kvp(msg.getBodyString()); 
			String tokenstr = null;
			if(data.containsKey(Protocol.TOKEN)) {
				tokenstr = data.get(Protocol.TOKEN);
			}
			Token token = authProvider.getToken(tokenstr); 
			
			Message res = new Message(); 
			if(token == null){
				res.setHeader("location", "/login"); 
				res.setStatus(302); 
				sess.write(res);
				return;
			} 
			
			if(token != null){
				Cookie cookie = new DefaultCookie(Protocol.TOKEN, tokenstr); 
				res.setHeader("Set-Cookie", ServerCookieEncoder.STRICT.encode(cookie));
			} 
			res.setHeader("location", "/"); 
			res.setStatus(302); //redirect to home page
			sess.write(res);
		}
	};  
	
	private MessageHandler<Message> logoutHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			Message res = new Message();  
			res.setId(msg.getId());
			res.setHeader("location", "/login"); 
			
			Cookie cookie = new DefaultCookie(Protocol.TOKEN, "");
			cookie.setMaxAge(0);
			res.setHeader("Set-Cookie", ServerCookieEncoder.STRICT.encode(cookie)); 
			res.setStatus(302); 
			sess.write(res); 
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
			model = FileKit.parseKeyValuePairs(params);
		}
		String body = null;
		try{
			body = FileKit.renderFile(fileName, model);
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
			body = FileKit.loadFileBytes(fileName);
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
	
	private MessageHandler<Message> pageHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException { 
			String url = msg.getUrl();
			if(url != null && !url.endsWith(".htm")){
				url += ".htm";
			}
			Message res = handleTemplateRequest("/page/", url);
			if("200".equals(res.getStatus())){
				res.setHeader("content-type", "text/html");
			}
			sess.write(res); 
		}
	};
	
	private MessageHandler<Message> jsHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/js/", msg.getUrl());
			if(res.getStatus() == 200){
				res.setHeader("content-type", "application/javascript");
			}
			sess.write(res); 
		}
	};
	
	private MessageHandler<Message> cssHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/css/", msg.getUrl());
			if(res.getStatus() == 200){
				res.setHeader("content-type", "text/css");
			} 
			sess.write(res);
		}
	}; 
	
	private MessageHandler<Message> imgHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/img/", msg.getUrl());
			if(res.getStatus() == 200){
				res.setHeader("content-type", "image/svg+xml");
			} 
			sess.write(res);
		}
	}; 
	
	private MessageHandler<Message> faviconHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/img/", "/img/favicon.ico");
			if(res.getStatus() == 200){
				res.setHeader("content-type", "image/x-icon");
			} 
			sess.write(res);
		}
	};  
	
	private MessageHandler<Message> sslHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException { 
			String server = msg.getHeader("server");
			if(StrKit.isEmpty(server)){
				server = mqServer.getServerAddress().address;
			}
			
			String certContent = mqServer.sslCertTable.get(server);
			if(certContent == null){
				ReplyKit.reply404(msg, sess, "Certificate("+server+") Not Found");
				return;
			}
			
			Message res = new Message();
			res.setId(msg.getId());
			res.setStatus(200); 
			res.setBody(certContent);
			sess.write(res); 
		}
	};
	
	private MessageHandler<Message> heartbeatHandler = new MessageHandler<Message>() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			// just ignore
		}
	}; 
	 
	private MessageHandler<Message> trackPubServerHandler = new MessageHandler<Message>() {
		
		@Override
		public void handle(Message msg, Session session) throws IOException {  
			try{
				boolean ack = msg.isAck(); 
				ServerEvent event = JsonKit.parseObject(msg.getBodyString(), ServerEvent.class);   
				if(event == null){
					ReplyKit.reply400(msg, session, Protocol.TRACK_PUB + " json required");
					return;
				}
				tracker.serverInTrackUpdated(event); 
				
				if(ack){
					ReplyKit.reply200(msg, session);
				}
			} catch (Exception e) { 
				ReplyKit.reply500(msg, session, e);
			} 
		}
	};
	 
	private MessageHandler<Message> trackSubHandler = new MessageHandler<Message>() {
		
		@Override
		public void handle(Message msg, Session session) throws IOException { 
			tracker.clientSubcribe(msg, session);
		}
	}; 
	
	private MessageHandler<Message> trackerHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(Message msg, Session session) throws IOException { 
			Token token = authProvider.getToken(msg.getToken()); 
			ReplyKit.replyJson(msg, session, tracker.trackerInfo(token)); 
		}
	}; 
	
	private MessageHandler<Message> serverHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			Token token = authProvider.getToken(msg.getToken()); 
			ReplyKit.replyJson(msg, sess, tracker.serverInfo(token)); 
		}
	}; 
	
	protected void cleanSession(Session sess) throws IOException{
		super.cleanSession(sess);
		
		String topic = sess.attr(Protocol.TOPIC);
		if(topic != null){
			MessageQueue mq = mqTable.get(topic); 
			if(mq != null){
				mq.cleanSession(sess); 
				tracker.myServerChanged();
			}
		}
		
		tracker.cleanSession(sess);  
	} 
	
    public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}  
    
	public void loadDiskQueue() throws IOException {
		log.info("Loading DiskQueues...");
		mqTable.clear();
		
		File[] mqDirs = new File(config.getMqPath()).listFiles(new FileFilter() {
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
    	
    	msg.setBody(JsonKit.toJSONString(req));
    }
    
    private void handleUrlMessage(Message msg){ 
    	if(msg.getCommand() != null){
    		return;
    	} 
    	String url = msg.getUrl(); 
    	if(url == null || "/".equals(url)){
    		msg.setCommand(Protocol.HOME);
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
    		String topicGroup = cmd.substring(idx+1);
    		cmd = cmd.substring(0, idx);
    		if(restUrlCommands.contains(cmd)){
    			String[] bb = topicGroup.split("[/]"); 
    			int i = -1;
    			while(++i < bb.length){
    				if(bb[i].length() == 0) continue; 
    				String topic = bb[i];
    				if(msg.getTopic() == null){
    					msg.setTopic(topic);
    				}
    				break;
    			} 
    			while(++i < bb.length){
    				if(bb[i].length() == 0) continue;
    				String group = bb[i];
    				if(msg.getConsumeGroup() == null){
    					msg.setConsumeGroup(group);
    				}
    				break;
    			}  
    		}
    	}
    	
    	msg.setCommand(cmd.toLowerCase());
    	//handle RPC
    	if(Protocol.RPC.equalsIgnoreCase(cmd) && msg.getBody() == null){ 
    		handlUrlRpcMessage(msg); 
    	}
    	msg.urlToHead();  
	}
    
    private void parseCookieToken(Message msg){
    	String cookieString = msg.getHeader("cookie");
        if (cookieString != null) {
            Cookie cookie = ClientCookieDecoder.STRICT.decode(cookieString);
            if(Protocol.TOKEN.equals(cookie.name().toLowerCase())){
            	if(msg.getToken() == null){
            		msg.setToken(cookie.value());
            	}
            	msg.removeHeader("cookie");
            } 
        }
    }
     
    
    public void onMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	msg.setSender(sess.id());
		msg.setHost(mqServer.getServerAddress().address); 
		msg.setRemoteAddr(sess.remoteAddress());
		if(msg.getId() == null){
			msg.setId(UUID.randomUUID().toString());
		}
		parseCookieToken(msg);
		
		if(verbose){
			log.info("\n%s", msg);
		} 
		
		
		handleUrlMessage(msg); 
		
		String cmd = msg.getCommand();
		boolean auth = true;
		if(!Protocol.LOGIN.equals(cmd)){
			auth = authProvider.auth(msg);
		}
		
		if(!auth){ 
			if(Protocol.HOME.equals(cmd)){
				ReplyKit.reply302(msg, sess, "/login");
			} else { 
				ReplyKit.reply403(msg, sess);
			} 
			return;
		} 
		
    	if(cmd != null){
    		MessageHandler<Message> handler = handlerMap.get(cmd);
	    	if(handler != null){
	    		handler.handle(msg, sess);
	    		return;
	    	}
    	}
    	
    	String topic = cmd; //treat cmd as topic to support proxy URL
    	MessageQueue mq = null;
    	if(topic != null){
    		mq = mqTable.get(topic); 
    	}
    	if(mq == null || cmd == null){
    		Message res = new Message();
        	res.setId(msg.getId()); 
        	res.setStatus(400);
        	String text = String.format("Bad format: command(%s) not support", cmd);
        	res.setBody(text); 
        	sess.write(res);
        	return;
    	}
    	  
    	msg.setTopic(topic);
    	msg.setCommand(Protocol.PRODUCE);
    	msg.setAck(false);
    	produceHandler.handle(msg, sess);  
    }  
	
    private MessageQueue findMQ(Message msg, Session sess) throws IOException{
		String topic = msg.getTopic();
		if(topic == null){
			ReplyKit.reply400(msg, sess, "Missing topic"); 
    		return null;
		}
		
		MessageQueue mq = mqTable.get(topic); 
    	if(mq == null){
    		ReplyKit.reply404(msg, sess); 
    		return null;
    	} 
    	return mq;
	}

	private boolean validateMessage(Message msg, Session session) throws IOException{
		final boolean ack = msg.isAck();  
		String id = msg.getId();
		String tag = msg.getTag();
		if(id != null && id.length()>DiskMessage.ID_MAX_LEN){ 
			if(ack) ReplyKit.reply400(msg, session, "Message.Id length should <= "+DiskMessage.ID_MAX_LEN);
			return false;
		}
		if(tag != null && tag.length()>DiskMessage.TAG_MAX_LEN){ 
			if(ack) ReplyKit.reply400(msg, session, "Message.Tag length should <= "+DiskMessage.TAG_MAX_LEN);
			return false;
		}
		return true;
	}
    
    public void registerHandler(String command, MessageHandler<Message> handler){
    	this.handlerMap.put(command, handler);
    } 
}