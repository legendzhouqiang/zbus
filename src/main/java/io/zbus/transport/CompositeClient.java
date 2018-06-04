package io.zbus.transport;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.zbus.auth.RequestSign;

public class CompositeClient extends AbastractClient  { 
	protected AbastractClient support; 
	
	public void sendMessage(Map<String, Object> data) {
		support.sendMessage(data);
	}
	
	public void connect(){ 
		support.connect();
	}
	
	public synchronized void heartbeat(long interval, TimeUnit timeUnit, MessageBuilder builder) {
		support.heartbeat(interval, timeUnit, builder);
	}  
	
	@Override
	public void close() throws IOException { 
		support.close();
	} 

	public void invoke(Map<String, Object> req, DataHandler<Map<String, Object>> dataHandler) {
		support.invoke(req, dataHandler);
	}

	public void invoke(Map<String, Object> req, DataHandler<Map<String, Object>> dataHandler,
			ErrorHandler errorHandler) {
		support.invoke(req, dataHandler, errorHandler);
	}

	public Map<String, Object> invoke(Map<String, Object> req) throws IOException, InterruptedException {
		return support.invoke(req);
	}

	public Map<String, Object> invoke(Map<String, Object> req, long timeout, TimeUnit timeUnit)
			throws IOException, InterruptedException {
		return support.invoke(req, timeout, timeUnit);
	}

	public boolean handleInvokeResponse(Map<String, Object> response) throws Exception {
		return support.handleInvokeResponse(response);
	}; 

	public void setApiKey(String apiKey) {
		support.setApiKey(apiKey);
	}

	public void setSecretKey(String secretKey) {
		support.setSecretKey(secretKey);
	}

	public void setAuthEnabled(boolean authEnabled) {
		support.setAuthEnabled(authEnabled);
	}

	public void setRequestSign(RequestSign requestSign) {
		support.setRequestSign(requestSign);
	}

	public void onMessage(DataHandler<Map<String, Object>> onMessage) {
		support.onMessage(onMessage);
	}

	public void onClose(EventHandler onClose) {
		support.onClose(onClose);
	}

	public void onOpen(EventHandler onOpen) {
		support.onOpen(onOpen);
	}

	public void onError(ErrorHandler onError) {
		support.onError(onError);
	}

	public void setReconnectDelay(int reconnectDelay) {
		support.setReconnectDelay(reconnectDelay);
	}
} 
