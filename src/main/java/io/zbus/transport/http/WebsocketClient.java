package io.zbus.transport.http;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.auth.DefaultSign;
import io.zbus.auth.RequestSign;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.Protocol;
import io.zbus.transport.DataHandler;
import io.zbus.transport.ErrorHandler;
import io.zbus.transport.EventHandler;
import io.zbus.transport.Invoker;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebsocketClient implements Invoker, Closeable {
	private static final Logger logger = LoggerFactory.getLogger(WebsocketClient.class); 
	public String apiKey;
	public String secretKey;
	public boolean authEnabled = false;
	public RequestSign requestSign = new DefaultSign(); 
	
	public DataHandler<String> onText;
	public DataHandler<ByteBuffer> onBinary;
	public EventHandler onClose;
	public EventHandler onOpen;
	public ErrorHandler onError;
	
	public int reconnectDelay = 3000; // 3s 
	public long lastActiveTime = System.currentTimeMillis();
	
	protected Map<String, RequestContext> callbackTable = new ConcurrentHashMap<>(); //id->context
	
	private OkHttpClient client; 
	private String address;
	private WebSocket ws;
	private Object wsLock = new Object();
	
	private List<String> cachedSendingMessages = new ArrayList<String>();  
	
	public WebsocketClient(String address, OkHttpClient client) { 
		this.client = client;
		if(!address.startsWith("ws://") && !address.startsWith("wss://")) {
			address = "ws://" + address;
		}
		this.address = address; 
		
		onText = msg-> { 
			@SuppressWarnings("unchecked")
			Map<String, Object> response = JsonKit.parseObject(msg, Map.class); 
			onResponse(response); 
		} ;
		
		onClose = ()-> {
			synchronized (wsLock) {
				if(ws != null){
					ws.close(1000, null); 
					ws = null;
				}
			}; 
			
			try {
				logger.info("Trying to reconnect " + WebsocketClient.this.address);
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
	
	public WebsocketClient(String address){
		this(address, new OkHttpClient());  
	} 
	
	
	@Override
	public void close() throws IOException { 
		onClose = null;
		onError = null;
		
		synchronized (this.wsLock) {
			if(this.ws != null){
				this.ws.close(1000, null);  
				this.ws = null;
			} 
		} 
	} 
	
	public void sendMessage(String command){
		synchronized (wsLock) {
			if(this.ws == null){
				this.cachedSendingMessages.add(command);
				this.connect();
				return;
			} 
			this.ws.send(command);
		}  
	}  
	
	public void invoke(Map<String, Object> req, 
			DataHandler<Map<String, Object>> dataHandler) {
		invoke(req, dataHandler, null);
	}
	
	public void invoke(Map<String, Object> req, 
			DataHandler<Map<String, Object>> dataHandler,
			ErrorHandler errorHandler) {
		
		String id = (String)req.get("id");
		if(id == null) {
			id = StrKit.uuid();
			req.put("id", id); 
		}  
		if(authEnabled) {
			if(apiKey == null) {
				throw new IllegalStateException("apiKey not set");
			}
			if(secretKey == null) {
				throw new IllegalStateException("secretKey not set");
			}
			
			requestSign.sign(req, apiKey, secretKey);
		} 
		
		RequestContext ctx = new RequestContext(req, dataHandler, errorHandler); 
		callbackTable.put(id, ctx); 
		
		sendMessage(JsonKit.toJSONString(req));
	}   
	
	public Map<String, Object> invoke(Map<String, Object> req) throws IOException, InterruptedException { 
		return invoke(req, 10, TimeUnit.SECONDS);
	}
	
	public Map<String, Object> invoke(Map<String, Object> req, long timeout, TimeUnit timeUnit) throws IOException, InterruptedException { 
		CountDownLatch countDown = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> res = new AtomicReference<Map<String, Object>>();  
		long start = System.currentTimeMillis();
		invoke(req, data->{ 
			res.set(data);
			countDown.countDown();
		});
		countDown.await(timeout, timeUnit);
		if(res.get() == null){ 
			long end = System.currentTimeMillis();
			String msg = String.format("Timeout(Time=%dms, ID=%s): %s", (end-start), (String)req.get("id"), JsonKit.toJSONString(req)); 
			throw new IOException(msg);
		}
		return res.get();
	} 
	
	public synchronized void connect(){    
		connectUnsafe();
	}
	
	protected void connectUnsafe(){   
		lastActiveTime = System.currentTimeMillis(); 
		Request request = new Request.Builder()
				.url(address)
				.build(); 
		
		this.ws = client.newWebSocket(request, new WebSocketListener() {
			@Override
			public void onOpen(WebSocket webSocket, Response response) {
				String msg = String.format("Websocket(%s) connected", address);
				logger.info(msg);
				response.close();
				
				if(cachedSendingMessages.size()>0){
					for(String json : cachedSendingMessages){
						sendMessage(json);
					}
					cachedSendingMessages.clear();
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
				try {
					if(onText != null){
						onText.handle(text);
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
				String msg = String.format("Websocket(%s) closed", address);
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
				String error = String.format("Websocket(%s) error: %s", address, t.getMessage());
				logger.error(error);
				if(response != null) {
					response.close();
				}
				if(onError != null){
					onError.handle(t);
				}
			} 
		}); 
	} 
	
	protected boolean onResponse(Map<String, Object> response) throws Exception { 
		String id = (String)response.get(Protocol.ID);
		if(id != null) {
			RequestContext ctx = callbackTable.remove(id);
			if (ctx != null) {  //1) Request-Response invocation
				Integer status = (Integer)response.get(Protocol.STATUS);
				if(status != null && status != 200) { 
					if(ctx.onError != null) {
						ctx.onError.handle(new RuntimeException((String)response.get(Protocol.BODY)));
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
				return true;
			} 
		}  
		return false;
	}; 
	
	public static class RequestContext {
		public Map<String, Object> request;
		public DataHandler<Map<String, Object>> onData;
		public ErrorHandler onError;
		
		RequestContext(Map<String, Object> request, DataHandler<Map<String, Object>> onData, ErrorHandler onError){
			this.request = request;
			this.onData = onData;
			this.onError = onError;
		}
	} 
}
