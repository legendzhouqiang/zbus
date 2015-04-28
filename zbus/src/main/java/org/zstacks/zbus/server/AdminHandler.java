package org.zstacks.zbus.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zstacks.zbus.protocol.MessageMode;
import org.zstacks.zbus.protocol.Proto;
import org.zstacks.zbus.server.mq.MessageQueue;
import org.zstacks.zbus.server.mq.PubsubQueue;
import org.zstacks.zbus.server.mq.RequestQueue;
import org.zstacks.zbus.server.mq.store.MessageStore;
import org.zstacks.znet.Helper;
import org.zstacks.znet.Message;
import org.zstacks.znet.MessageHandler;
import org.zstacks.znet.nio.Session;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class AdminHandler extends SubCommandHandler {
	private static final Logger log = LoggerFactory.getLogger(AdminHandler.class); 
	private final ConcurrentMap<String, MessageQueue> mqTable;
	private final ExecutorService mqExecutor;
	private final String serverAddr;
	private final TrackReport trackReport;
	
	private MessageStore messageStore = null; 
	
	public AdminHandler(ConcurrentMap<String, MessageQueue> mqTable, 
			ExecutorService mqExecutor, String serverAddr,
			TrackReport trackReport){
		this.mqTable = mqTable;
		this.mqExecutor = mqExecutor;
		this.serverAddr = serverAddr;
		this.trackReport = trackReport;
		this.initCommands();
	}
	
	private void initCommands(){

		this.registerHandler(Proto.AdminCreateMQ, new MessageHandler() { 
			public void handleMessage(Message msg, Session sess) throws IOException { 
				JSONObject params = null;
				try{
					params = JSON.parseObject(msg.getBodyString());
				} catch(Exception e) {
					log.error(e.getMessage(), e);
					msg.setBody("register param json body invalid");
	    			ServerHelper.reply400(msg, sess);
	        		return; 
				}
				
				
				String msgId= msg.getMsgId();
				String mqName = params.getString("mqName");
	    		String accessToken = params.getString("accessToken");
	    		String type = params.getString("mqMode");
	    		int mode = 0;
	    		try{
	    			mode = Integer.valueOf(type);
	    		} catch (Exception e){
	    			msg.setBody("mqMode invalid");
	    			ServerHelper.reply400(msg, sess);
	        		return;  
	    		}
	    		
	    		
	    		if(mqName == null){
	    			msg.setBody("Missing mq_name filed");
	    			ServerHelper.reply400(msg, sess);
	        		return;  
	    		} 
	    		
	    		MessageQueue mq = null;	
	    		synchronized (mqTable) {
	    			mq = mqTable.get(mqName);
	    			if(mq == null){ 
		    			if(MessageMode.isEnabled(mode, MessageMode.PubSub)){
		    				mq = new PubsubQueue(serverAddr, mqName, mqExecutor, mode);
		    				mq.setAccessToken(accessToken);
		    				mq.setCreator(sess.getRemoteAddress());
		    			} else {//默认到消息队列
		    				mq = new RequestQueue(serverAddr, mqName, mqExecutor, mode);
		    				mq.setMessageStore(messageStore);
		    				mq.setAccessToken(accessToken);
		    				mq.setCreator(sess.getRemoteAddress());
		    				if(messageStore != null){
		    					messageStore.onMessageQueueCreated(mq);
		    				}
		    			}  
			    		mqTable.putIfAbsent(mqName, mq);
						log.info("MQ Created: {}", mq);
						ServerHelper.reply200(msgId, sess); 
						
			    		trackReport.reportToTrackServer();
			    		return;
		    		}
	    		}
	    		
	    		if(MessageMode.isEnabled(mode, MessageMode.MQ) && !(mq instanceof RequestQueue)){
    				msg.setBody("MsgQueue, type not matched");
	    			ServerHelper.reply400(msg, sess);
	        		return;  
    			}
	    		if(MessageMode.isEnabled(mode, MessageMode.PubSub) && !(mq instanceof PubsubQueue)){
    				msg.setBody("Pubsub, type not matched");
	    			ServerHelper.reply400(msg, sess);
	        		return;  
    			}
    			ServerHelper.reply200(msgId, sess);  
			}
		}); 
		
		this.registerHandler("", new MessageHandler() { 
			public void handleMessage(Message msg, Session sess) throws IOException {
				msg = new Message();
				msg.setStatus("200");
				msg.setHead("content-type","text/html");
				String body = Helper.loadFileContent("zbus.htm"); 
				msg.setBody(body); 
				sess.write(msg);  
			}
		});
		
		this.registerHandler("jquery", new MessageHandler() { 
			public void handleMessage(Message msg, Session sess) throws IOException {
				msg = new Message();
				msg.setStatus("200");
				msg.setHead("content-type","application/javascript");
				String body = Helper.loadFileContent("jquery.js"); 
				msg.setBody(body); 
				sess.write(msg);  
			}
		});
		
		this.registerHandler("data", new MessageHandler() { 
			public void handleMessage(Message msg, Session sess) throws IOException {
				msg = trackReport.packServerInfo();
				msg.setStatus("200"); 
				msg.setHead("content-type", "application/json");
				sess.write(msg);  
			}
		});
	}
	 
	public MessageStore getMessageStore() {
		return messageStore;
	}

	public void setMessageStore(MessageStore messageStore) {
		this.messageStore = messageStore;
	}
	
}


class SubCommandHandler implements MessageHandler {   
	protected String accessToken = ""; 
	protected Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<String, MessageHandler>();

	
	public void registerHandler(String command, MessageHandler handler){
    	this.handlerMap.put(command, handler);
    }
	
	public void handleMessage(Message msg, Session sess) throws IOException {
		if(!accessToken.equals("") && !accessToken.equals(msg.getToken())){
    		ServerHelper.reply403(msg, sess);
    		return;
    	}
		String subCmd = msg.getSubCommand(); 
		if(subCmd == null){
			subCmd = "";
		}
		
		MessageHandler handler = this.handlerMap.get(subCmd);
		if(handler == null){  
			msg.setBody("sub_cmd=%s Not Found", subCmd);
			ServerHelper.reply404(msg, sess);
    		return; 
		} 
		handler.handleMessage(msg, sess);
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String value) {
		this.accessToken = value;
	} 
	
}

