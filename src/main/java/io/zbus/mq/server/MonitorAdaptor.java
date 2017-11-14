package io.zbus.mq.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.zbus.kit.FileKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.Message;
import io.zbus.mq.Protocol;
import io.zbus.mq.server.auth.AuthProvider;
import io.zbus.mq.server.auth.Token;
import io.zbus.transport.MessageHandler;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session; 

public class MonitorAdaptor extends ServerAdaptor implements Closeable { 
 
	private final Map<String, MessageHandler<Message>> handlerMap = new ConcurrentHashMap<String, MessageHandler<Message>>();
	private AuthProvider authProvider;
	 
	public MonitorAdaptor(MqServer mqServer){
		super(mqServer.getSessionTable()); 
		this.authProvider = mqServer.getConfig().getAuthProvider();
		 
		//Monitor/Management
		registerHandler(Protocol.HOME, homeHandler);  
		registerHandler("favicon.ico", faviconHandler);
		
		registerHandler(Protocol.LOGIN, loginHandler);  
		registerHandler(Protocol.LOGOUT, logoutHandler);  
		registerHandler(Protocol.JS, jsHandler); 
		registerHandler(Protocol.CSS, cssHandler);
		registerHandler(Protocol.IMG, imgHandler); 
		registerHandler(Protocol.PAGE, pageHandler);     
		
		registerHandler(Message.HEARTBEAT, heartbeatHandler);     
	}   
	 
	 
	
	private MessageHandler<Message> homeHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			String tokenStr = msg.getToken();
			Token token = authProvider.getToken(tokenStr);
			Map<String, Object> model = new HashMap<String, Object>();
			String tokenShow = null;
			if(token != null && tokenStr != null){
				tokenShow = String.format("<li><a href='/?cmd=logout'>%s Logout</a></li>", token.name);
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
			
			Map<String, String> data = StrKit.kvp(msg.getBodyString(), "&"); 
			String tokenstr = null;
			if(data.containsKey(Protocol.TOKEN)) {
				tokenstr = data.get(Protocol.TOKEN);
			}
			Token token = authProvider.getToken(tokenstr); 
			
			Message res = new Message(); 
			if(token == null){
				res.setHeader("location", "/?cmd=login"); 
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
			res.setHeader("location", "/?cmd=login"); 
			
			Cookie cookie = new DefaultCookie(Protocol.TOKEN, "");
			cookie.setMaxAge(0);
			res.setHeader("Set-Cookie", ServerCookieEncoder.STRICT.encode(cookie)); 
			res.setStatus(302); 
			sess.write(res); 
		}
	};  
	
	private Message handleTemplateRequest(Message msg){
		return handleTemplateRequest(msg, null);
	}
	
	private Message handleTemplateRequest(Message msg, Map<String, Object> model){
		Message res = new Message();  
		String fileName = msg.getTopic();
		String cmd = msg.getCommand();  
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
		if(Protocol.JS.equals(cmd)){
			res.setHeader("content-type", "application/javascript");
		} else if(Protocol.CSS.equals(cmd)){
			res.setHeader("content-type", "text/css");
		} else if(Protocol.IMG.equals(cmd)){
			if("favicon.ico".equals(fileName)){
				res.setHeader("content-type", "image/x-icon");
			} else {
				res.setHeader("content-type", "image/svg+xml");
			}
		} else {
			res.setHeader("content-type", "text/html");
		}
		return res;
	}
	
	 
	
	private MessageHandler<Message> pageHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			Message res = handleTemplateRequest(msg); 
			sess.write(res); 
		}
	};
	
	private MessageHandler<Message> jsHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleTemplateRequest(msg); 
			sess.write(res); 
		}
	};
	
	private MessageHandler<Message> cssHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleTemplateRequest(msg); 
			sess.write(res); 
		}
	}; 
	
	private MessageHandler<Message> imgHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleTemplateRequest(msg); 
			sess.write(res); 
		}
	}; 
	
	private MessageHandler<Message> faviconHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleTemplateRequest(msg); 
			sess.write(res); 
		}
	};  
	 
	private MessageHandler<Message> heartbeatHandler = new MessageHandler<Message>() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			// just ignore
		}
	};   
     
    
    public void onMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;   
		msg.parseCookieToken();
		
		String cmd = msg.getCommand();
		boolean auth = authProvider.auth(msg); 
		if(!auth){ 
			if(Protocol.HOME.equals(cmd)){
				ReplyKit.reply302(msg, sess, "/?cmd=login");
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
    }   
    
    public void registerHandler(String command, MessageHandler<Message> handler){
    	this.handlerMap.put(command, handler);
    }

	@Override
	public void close() throws IOException { 
		
	}  
}