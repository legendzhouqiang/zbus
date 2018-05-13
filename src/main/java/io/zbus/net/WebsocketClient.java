package io.zbus.net;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebsocketClient implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(WebsocketClient.class); 
	
	public DataHandler<JSONObject> onText;
	public DataHandler<ByteBuffer> onBinary;
	public EventHandler onClose;
	public EventHandler onOpen;
	public ErrorHandler onError;
	
	public int reconnectDelay = 3000; // 3s 
	public long lastActiveTime = System.currentTimeMillis();
	
	private OkHttpClient client; 
	private String streamUrl;
	private WebSocket ws;
	private Object wsLock = new Object();
	
	private List<JSONObject> cachedCommands = new ArrayList<JSONObject>(); 
	
	public WebsocketClient(String streamUrl, OkHttpClient client) { 
		this.client = client;
		if(!streamUrl.startsWith("ws://") && !streamUrl.startsWith("wss://")) {
			streamUrl = "ws://" + streamUrl;
		}
		this.streamUrl = streamUrl; 
		
		onClose = ()-> {
			synchronized (wsLock) {
				if(ws != null){
					ws.cancel(); 
					ws = null;
				}
			}; 
			
			try {
				logger.info("Trying to reconnect " + WebsocketClient.this.streamUrl);
				Thread.sleep(reconnectDelay);
			} catch (InterruptedException e) {
				// ignore
			}  
			connect();
		};
		
		onError = e -> {;
			if(onClose != null){
				try {
					onClose.handle();
				} catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
		};
	}
	
	public WebsocketClient(String streamUrl){
		this(streamUrl, new OkHttpClient()); 
	}
	
	@Override
	public void close() throws IOException { 
		onClose = null;
		onError = null;
		if(this.ws != null){
			this.ws.cancel();
			this.ws = null;
		} 
	} 
	
	public void sendMessage(JSONObject command){
		synchronized (wsLock) {
			if(this.ws == null){
				this.cachedCommands.add(command);
				return;
			} 
			this.ws.send(JSON.toJSONString(command));
		}  
	} 
	
	public void connect(){   
		lastActiveTime = System.currentTimeMillis();
		synchronized (wsLock) {
			if(this.ws != null) return; //already connected
		} 
		
		Request request = new Request.Builder()
				.url(streamUrl)
				.build(); 
		
		this.ws = client.newWebSocket(request, new WebSocketListener() {
			@Override
			public void onOpen(WebSocket webSocket, Response response) {
				String msg = String.format("Websocket(%s) connected", streamUrl);
				logger.info(msg);
				if(cachedCommands.size()>0){
					for(JSONObject json : cachedCommands){
						sendMessage(json);
					}
				} 
				if(onOpen != null){
					try {
						onOpen.handle();
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			}

			@Override
			public void onMessage(WebSocket webSocket, String text) {
				lastActiveTime = System.currentTimeMillis();
				JSONObject json = JSON.parseObject(text);
				try {
					if(onText != null){
						onText.handle(json);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
			
			@Override
			public void onMessage(WebSocket webSocket, ByteString bytes) {
				lastActiveTime = System.currentTimeMillis();
				try {
					if(onBinary != null){
						onBinary.handle(bytes.asByteBuffer());
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}

			@Override
			public void onClosed(WebSocket webSocket, int code, String reason) { 
				String msg = String.format("Websocket(%s) closed", streamUrl);
				logger.info(msg);
				if(onClose != null){
					try {
						onClose.handle();
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			}

			@Override
			public void onFailure(WebSocket webSocket, Throwable t, Response response) { 
				String error = String.format("Websocket(%s) error: %s", streamUrl, t.getMessage());
				logger.warn(error);
				
				if(onError != null){
					onError.handle(t);
				}
			} 
		}); 
	}
}
