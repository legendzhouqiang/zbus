package org.zbus.examples.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

public class UrlService extends Service { 
	private UrlProcessor urlProcessor;
	private Broker broker;
	
	public UrlService(String brokerAddress, String entryId) throws IOException {
		this.config = new ServiceConfig();
		config.setMq(entryId);
		broker = new ZbusBroker(brokerAddress);
		config.setBroker(broker);
		
		urlProcessor = new UrlProcessor(config.getMq());
		config.setMessageProcessor(urlProcessor); 
	}
	
	public UrlService(ServiceConfig config) {
		super(config); 
		urlProcessor = new UrlProcessor(config.getMq());
		config.setMessageProcessor(urlProcessor);
	}
	
	public void url(String path, final MessageProcessor processor) {
		this.urlProcessor.url(path, processor);
	}
	
	
	@Override
	public void close() throws IOException { 
		super.close();
		if(broker != null){
			broker.close();
			broker = null;
		}
	}
	
	static class UrlProcessor implements MessageProcessor {
		private Map<String, MessageProcessor> urlHandlerMap = new ConcurrentHashMap<String, MessageProcessor>();
		private String entryId = ""; 
		public UrlProcessor(String entryId){
			this.entryId = entryId;
		}
		
		@Override
		public Message process(Message msg) {
			final String msgId = msg.getId();
			String url = msg.removeHead("origin-url");
			if(url == null){
		    	Message res = new Message();
		    	res.setId(msgId); 
		    	res.setStatus(400);
		    	String text = String.format("Missing origin-url from zbus broker");
		    	res.setBody(text);  
				return res;
			}
			
			msg.setUrl(url);
			String path = msg.getRequestPath();
			MessageProcessor urlHandler = urlHandlerMap.get(path);
	    	if(urlHandler != null){
	    		Message res = null; 
	    		try{
	    			res = urlHandler.process(msg); 
		    		if(res != null){
		    			res.setId(msgId);
		    			if(res.getStatus() == null){
		    				res.setStatus(200);// default to 200
		    			} 
		    		}
	    		} catch (Exception e) { 
	    			res = new Message();
	    			res.setStatus(500);
	    			res.setBody("Internal Error(500): " + e); 
				}
	    
	    		return res;
	    	} 
	    	
	    	Message res = new Message();
	    	res.setId(msgId); 
	    	res.setStatus(404);
	    	String text = String.format("Not Found(404): %s", path);
	    	res.setBody(text);  
			return res;
		} 

		public void url(String path, final MessageProcessor processor) {
			if(!path.startsWith("/")){
				path = "/" + path;
			} 
			if(!"".equals(entryId)){
				path = "/" + entryId + path;
			}
			
			this.urlHandlerMap.put(path, processor);
		}
	}
}
