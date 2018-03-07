package io.zbus.net.http;
 

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.net.Client;
import io.zbus.net.ErrorHandler;
import io.zbus.net.EventLoop;
import io.zbus.net.MessageHandler;

public class HttpClient extends Client<HttpMsg, HttpMsg> {
	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
	private boolean traceEnabled = true;
	private boolean autoClose = false; 
	
	private long defaultTimeout = 10000; //10s
	
	public HttpClient(URI uri, final EventLoop loop) {
		super(uri, loop);
		setup();
	}
	
	public HttpClient(String address, final EventLoop loop) {
		super(address, loop);
		setup();
	} 
	
	private void setup(){
		codec(p -> {
			p.add(new HttpRequestEncoder());
			p.add(new HttpResponseDecoder());
			p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
			p.add(new HttpMsgClientCodec());
		});

		onClose = null; // Disable auto reconnect
		onError = null;
	}
	
	private void fillCommonHeader(HttpMsg req){
		req.setHeader("host", host);
	}

	public HttpMsg request(HttpMsg req) throws IOException, InterruptedException {
		fillCommonHeader(req);
		return request(req, defaultTimeout);
	}

	public HttpMsg request(HttpMsg req, long timeout) throws IOException, InterruptedException {
		fillCommonHeader(req);
		
		final long start = System.currentTimeMillis(); 
		URI uri = req.getUri();
		if(uri == null){
			uri = this.uri;
		}
		final String reqStr = String.format("%s %s", req.getMethod() , uri.toString() + req.getUrl()); 
		if(traceEnabled){ 
			logger.info("Request(ID=%d) %s", start, reqStr);
		} 
		
		CountDownLatch countDown = new CountDownLatch(1);
		AtomicReference<HttpMsg> res = new AtomicReference<HttpMsg>();
		onMessage = resp -> {
			if(traceEnabled){
				long end = System.currentTimeMillis();
				String bodyLog = resp.getBodyString();
				int maxLength = 1024;
				if(bodyLog.length() > maxLength){
					bodyLog = bodyLog.substring(0, maxLength) + " ... [more ignored]";
				}
				logger.info("Resopnse(ID=%d, Time=%dms): %d\n%s", start, (end-start), resp.getStatus(), bodyLog);
			} 
			
			res.set(resp);
			countDown.countDown();
		}; 
		
		if(active()){
			sendMessage(req);
		} else {
			onOpen = ()->{ 
				sendMessage(req);
			};  
			connect(); 
		}
		
		countDown.await(timeout, TimeUnit.MILLISECONDS);
		if(res.get() == null){ 
			long end = System.currentTimeMillis();
			String msg = String.format("Timeout(Time=%dms, ID=%d): %s", (end-start), start, reqStr);
			if(traceEnabled){ 
				logger.error(msg);
			} 
			throw new IOException(msg);
		}
		return res.get();
	}

	
	public void request(HttpMsg req, final MessageHandler<HttpMsg> messageHandler,
			final ErrorHandler errorHandler) {
		fillCommonHeader(req);
		final AtomicLong start = new AtomicLong(System.currentTimeMillis());  
		if(traceEnabled){
			String url = req.getUrl();
			logger.info("AsyncRequest(ID=%d) %s %s", start.get(), req.getMethod(), url);
		}
		
		onMessage = msg->{
			if(traceEnabled){
				long end = System.currentTimeMillis(); 
				String bodyLog = msg.getBodyString();
				int maxLength = 1024;
				if(bodyLog.length() > maxLength){
					bodyLog = bodyLog.substring(0, maxLength) + " ... [more ignored]";
				}
				logger.info("AsyncResopnse(ID=%d, Time=%dms): %d\n%s", start.get(), (end-start.get()), msg.getStatus(), bodyLog);
			}
			if(autoClose){
				try {
					close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
			if(messageHandler != null){
				messageHandler.handle(msg);
			}
		};
		
		onError = e-> {
			long end = System.currentTimeMillis();  
			if(traceEnabled){
				String msg = String.format("AsyncResopnse(Time=%dms, ID=%d)", (end-start.get()), start.get());
				logger.error(msg, e);  
			}
			if(autoClose){
				try {
					close();
				} catch (IOException ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
			if(errorHandler != null){
				errorHandler.handle(e);
			}
		};
		
		onOpen = ()->{
			sendMessage(req);
		}; 
		
		connect();  
	}  
}
