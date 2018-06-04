package io.zbus.transport;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.auth.DefaultSign;
import io.zbus.auth.RequestSign;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.Protocol;

public abstract class AbastractClient implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(AbastractClient.class);
	
	protected String apiKey;
	protected String secretKey;
	protected boolean authEnabled = false;
	protected RequestSign requestSign = new DefaultSign();
	
	protected DataHandler<Map<String, Object>> onMessage; 
	protected EventHandler onClose;
	protected EventHandler onOpen;
	protected ErrorHandler onError;
	
	protected int reconnectDelay = 3000; // 3s 

	protected Map<String, RequestContext> callbackTable = new ConcurrentHashMap<>(); // id->context 
	protected ScheduledExecutorService heartbeator;
	
	public AbastractClient() {
		onMessage = msg-> {  
			handleInvokeResponse(msg); 
		}; 
		
		onClose = ()-> {  
			try { 
				Thread.sleep(reconnectDelay); //TODO make it async?
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
	
	public abstract void sendMessage(Map<String, Object> data);
	
	public void connect(){ 
		
	}
	
	public synchronized void heartbeat(long interval, TimeUnit timeUnit, MessageBuilder builder) {
		if(heartbeator == null) {
			heartbeator = Executors.newSingleThreadScheduledExecutor();
			heartbeator.scheduleAtFixedRate(()->{
				Map<String, Object> msg = builder.build(); 
				sendMessage(msg);
			}, interval, interval, timeUnit);
		}
	}  
	
	@Override
	public void close() throws IOException { 
		onClose = null;
		onError = null;
		
		if(heartbeator != null) {
			heartbeator.shutdown();
		}
	} 

	public void invoke(Map<String, Object> req, DataHandler<Map<String, Object>> dataHandler) {
		invoke(req, dataHandler, null);
	}

	public void invoke(Map<String, Object> req, DataHandler<Map<String, Object>> dataHandler,
			ErrorHandler errorHandler) {

		String id = (String) req.get("id");
		if (id == null) {
			id = StrKit.uuid();
			req.put("id", id);
		}
		if (authEnabled) {
			if (apiKey == null) {
				throw new IllegalStateException("apiKey not set");
			}
			if (secretKey == null) {
				throw new IllegalStateException("secretKey not set");
			}

			requestSign.sign(req, apiKey, secretKey);
		}

		RequestContext ctx = new RequestContext(req, dataHandler, errorHandler);
		callbackTable.put(id, ctx);

		sendMessage(req);
	}

	public Map<String, Object> invoke(Map<String, Object> req) throws IOException, InterruptedException {
		return invoke(req, 10, TimeUnit.SECONDS);
	}

	public Map<String, Object> invoke(Map<String, Object> req, long timeout, TimeUnit timeUnit)
			throws IOException, InterruptedException {
		CountDownLatch countDown = new CountDownLatch(1);
		AtomicReference<Map<String, Object>> res = new AtomicReference<Map<String, Object>>();
		long start = System.currentTimeMillis();
		invoke(req, data -> {
			res.set(data);
			countDown.countDown();
		});
		countDown.await(timeout, timeUnit);
		if (res.get() == null) {
			long end = System.currentTimeMillis();
			String msg = String.format("Timeout(Time=%dms, ID=%s): %s", (end - start), (String) req.get("id"),
					JsonKit.toJSONString(req));
			throw new IOException(msg);
		}
		return res.get();
	}

	public boolean handleInvokeResponse(Map<String, Object> response) throws Exception {
		String id = (String) response.get(Protocol.ID);
		if (id != null) {
			RequestContext ctx = callbackTable.remove(id);
			if (ctx != null) { // 1) Request-Response invocation
				Integer status = (Integer) response.get(Protocol.STATUS);
				if (status != null && status != 200) {
					if (ctx.onError != null) {
						ctx.onError.handle(new RuntimeException((String) response.get(Protocol.BODY)));
					} else {
						logger.error(JsonKit.toJSONString(response));
					}
				} else {
					if (ctx.onData != null) {
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

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public void setAuthEnabled(boolean authEnabled) {
		this.authEnabled = authEnabled;
	}

	public void setRequestSign(RequestSign requestSign) {
		this.requestSign = requestSign;
	}

	public void onMessage(DataHandler<Map<String, Object>> onMessage) {
		this.onMessage = onMessage;
	}

	public void onClose(EventHandler onClose) {
		this.onClose = onClose;
	}

	public void onOpen(EventHandler onOpen) {
		this.onOpen = onOpen;
	}

	public void onError(ErrorHandler onError) {
		this.onError = onError;
	}

	public void setReconnectDelay(int reconnectDelay) {
		this.reconnectDelay = reconnectDelay;
	}
 
	public static class RequestContext {
		public Map<String, Object> request;
		public DataHandler<Map<String, Object>> onData;
		public ErrorHandler onError;

		RequestContext(Map<String, Object> request, DataHandler<Map<String, Object>> onData, ErrorHandler onError) {
			this.request = request;
			this.onData = onData;
			this.onError = onError;
		}
	}
	
	public static interface MessageBuilder{
		Map<String, Object> build();
	}  
} 
