package io.zbus.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.auth.DefaultSign;
import io.zbus.auth.RequestSign;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.transport.DataHandler;
import io.zbus.transport.ErrorHandler;
import io.zbus.transport.http.WebsocketClient; 

public class MqClient extends WebsocketClient{
	private static final Logger logger = LoggerFactory.getLogger(MqClient.class); 
	public String apiKey;
	public String secretKey;
	public boolean authEnabled = false;
	public RequestSign requestSign = new DefaultSign();
	
	private Map<String, RequestContext> callbackTable = new ConcurrentHashMap<>(); //id->context
	private Map<String, Map<String,DataHandler<Map<String,Object>>>> handlerTable = new ConcurrentHashMap<>(); //mq=>{channel=>handler}
	
	private ScheduledExecutorService heartbeator;
	
	public MqClient(String address) { 
		super(address);
		
		onText = msg -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> response = JsonKit.parseObject(msg, Map.class); 
			String id = (String)response.get(Protocol.ID);
			if(id != null) {
				RequestContext ctx = callbackTable.remove(id);
				if (ctx != null) {  //1) Request-Response invocation
					Integer status = (Integer)response.get(Protocol.STATUS);
					if(status != 200) { 
						if(ctx.onError != null) {
							ctx.onError.handle(new RuntimeException((String)response.get(Protocol.DATA)));
						} else {
							logger.error(JsonKit.toJSONString(response)); 
						}
					} else {
						if(ctx.onData != null) {
							ctx.onData.handle(response);
						} else {
							logger.warn("Missing handler for: " + response);
						}
					}
					return;
				} 
			} 
			//Subscribed message pushing
			
			String mq = (String)response.get(Protocol.MQ);
			String channel = (String)response.get(Protocol.CHANNEL);
			if(mq == null || channel == null) {
				logger.warn("MQ/Channel both required in reponse: " + JsonKit.toJSONString(response));
				return;
			} 
			Map<String,DataHandler<Map<String,Object>>> mqHandlers = handlerTable.get(mq);
			if(mqHandlers == null) return;
			DataHandler<Map<String,Object>> handler = mqHandlers.get(channel);
			if(handler == null) return;
			
			handler.handle(response); 
		}; 
	}  
	
	public synchronized void heartbeat(long interval, TimeUnit timeUnit) {
		if(heartbeator == null) {
			heartbeator = Executors.newSingleThreadScheduledExecutor();
			heartbeator.scheduleAtFixedRate(()->{
				Map<String, Object> msg = new HashMap<>();
				msg.put(Protocol.CMD, Protocol.PING);
				sendMessage(JsonKit.toJSONString(msg));
			}, interval, interval, timeUnit);
		}
	}
	
	public void invoke(Map<String, Object> req, 
			DataHandler<Map<String, Object>> dataHandler) {
		invoke(req, dataHandler, null);
	}
	
	public void invoke(Map<String, Object> req, 
			DataHandler<Map<String, Object>> dataHandler,
			ErrorHandler errorHandler) {
		
		String id = (String)req.get(Protocol.ID);
		if(id == null) {
			id = StrKit.uuid();
			req.put(Protocol.ID, id); 
		} 
		
		RequestContext ctx = new RequestContext(req, dataHandler, errorHandler); 
		callbackTable.put(id, ctx); 
		
		sendMessage(JsonKit.toJSONString(req));
	}  
	
	public void publish(String mq, Object data) {
		Map<String, Object> req = new HashMap<>();
		req.put(Protocol.CMD, Protocol.PUB);
		req.put(Protocol.MQ, mq);
		req.put(Protocol.DATA, data);
		
		sendMessage(JsonKit.toJSONString(req));
	}
	
	public void publish(String mq, Object data, 
			DataHandler<Map<String, Object>> dataHandler) {
		publish(mq, data, dataHandler, null);
	} 
	
	public void publish(String mq, Object data, 
			DataHandler<Map<String, Object>> dataHandler,
			ErrorHandler errorHandler) {
		Map<String, Object> req = new HashMap<>();
		req.put(Protocol.CMD, Protocol.PUB);
		req.put(Protocol.MQ, mq);
		req.put(Protocol.DATA, data);
		
		invoke(req, dataHandler, errorHandler);
	} 
	
	public void subscribe(String mq, String channel, DataHandler<Map<String, Object>> dataHandler) {
		Map<String,DataHandler<Map<String,Object>>> mqHandlers = handlerTable.get(mq);
		if(mqHandlers == null) {
			mqHandlers = new ConcurrentHashMap<>();
			handlerTable.put(mq, mqHandlers);
		}
		mqHandlers.put(channel, dataHandler);
		
		Map<String, Object> req = new HashMap<>();
		req.put(Protocol.CMD, Protocol.SUB);
		req.put(Protocol.MQ, mq);
		req.put(Protocol.CHANNEL, channel);
		
		sendMessage(JsonKit.toJSONString(req));
	}
	
	@Override
	public void close() throws IOException { 
		super.close();
		if(heartbeator != null) {
			heartbeator.shutdown();
			heartbeator = null;
		}
	}
	
	static class RequestContext {
		Map<String, Object> request;
		DataHandler<Map<String, Object>> onData;
		ErrorHandler onError;
		
		RequestContext(Map<String, Object> request, DataHandler<Map<String, Object>> onData, ErrorHandler onError){
			this.request = request;
			this.onData = onData;
			this.onError = onError;
		}
	} 
}
