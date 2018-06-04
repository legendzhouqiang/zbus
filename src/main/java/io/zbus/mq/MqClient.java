package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.transport.Client;
import io.zbus.transport.DataHandler;
import io.zbus.transport.http.WebsocketClient; 

public class MqClient extends Client{ 
	private static final Logger logger = LoggerFactory.getLogger(MqClient.class); 
	private Map<String, Map<String,DataHandler<Map<String,Object>>>> handlerTable = new ConcurrentHashMap<>(); //mq=>{channel=>handler}
	
	public MqClient(String address) {  
		super(address);
		final WebsocketClient websocketClient = (WebsocketClient)support;
		websocketClient.onText = msg -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> response = JsonKit.parseObject(msg, Map.class); 
			boolean handled = support.handleInvokeResponse(response);
			if(handled) return;
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
	
	public MqClient(MqServer server) {
		super(server.getServerAdaptor());
	}
	 
	public synchronized void heartbeat(long interval, TimeUnit timeUnit) {
		heartbeat(interval, timeUnit, ()->{
			Map<String, Object> msg = new HashMap<>();
			msg.put(Protocol.CMD, Protocol.PING);
			return msg;
		});
	}  
	
	public void addListener(String mq, String channel, DataHandler<Map<String, Object>> dataHandler) {
		Map<String,DataHandler<Map<String,Object>>> mqHandlers = handlerTable.get(mq);
		if(mqHandlers == null) {
			mqHandlers = new ConcurrentHashMap<>();
			handlerTable.put(mq, mqHandlers);
		}
		mqHandlers.put(channel, dataHandler);
	}  
}
