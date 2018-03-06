package io.zbus.net.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.alibaba.fastjson.JSON;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.net.EventLoop; 

public class HttpInvoker {
	private static final Logger logger = LoggerFactory.getLogger(HttpInvoker.class);  
	private URI uri;
	private EventLoop loop;
	
	private long invokeTimeout = 10000;
	
	public HttpInvoker(EventLoop loop){
		this.loop = loop;
	}
	
	public HttpInvoker(String baseUrl, EventLoop loop){
		try {
			this.uri = new URI(baseUrl);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(baseUrl);
		}
		this.loop = loop;
	}
	
	public Object json(HttpMsg request) throws IOException {
		return json(request, this.invokeTimeout);
	}
	public <T> T object(HttpMsg request, Class<T> type) throws IOException {
		return object(request, type, invokeTimeout);
	}
	
	public Object json(HttpMsg request, long timeoutInMillis) throws IOException {
		String jsonstr = string(request, timeoutInMillis);
		return JSON.parse(jsonstr);
	}
	
	public <T> List<T> objectArray(HttpMsg request, Class<T> type) throws IOException {
		return objectArray(request, type, invokeTimeout);
	}
	
	public String string(HttpMsg request) throws IOException {
		return string(request, invokeTimeout);
	}
	
	
	public <T> T object(HttpMsg request, Class<T> type, long timeoutInMillis) throws IOException {
		String text = string(request, timeoutInMillis);
		return JSON.parseObject(text, type);
	}
	
	public <T> List<T> objectArray(HttpMsg request, Class<T> type, long timeoutInMillis) throws IOException {
		String text = string(request, timeoutInMillis);
		return JSON.parseArray(text, type);
	}

	public String string(HttpMsg request, long timeoutInMillis) throws IOException {
		checkRequest(request);
		URI uri = request.getUri();
		if(uri == null){
			uri = this.uri;
		}
		String url = uri.toString() + request.getUrl();
		url = "Request(" + url + ")"; 
		HttpClient client = new HttpClient(uri, loop);
		try{ 
			HttpMsg resp = client.request(request, timeoutInMillis);
			String body = resp.getBodyString();
			if (resp.getStatus() != 200) {
				url = "Error: " + body + " " + url;
				HttpException exception = new HttpException(url);  
				logger.error(exception.getMessage(), exception);
				throw exception;
			}
			return body;
		} catch (InterruptedException e) {
			return null; //ignore
		} catch (IOException e) {
			logger.error(url + " Error: ", e);
			throw e;
		} finally {
			if(client != null){
				client.close();
			}
		}   
	} 
	
	private void checkRequest(HttpMsg req){
		if(uri != null) return;
		if(req.getUri() == null) {
			throw new IllegalArgumentException("Missing uri for http request:\n" + req);
		}
	}
	 
	public void setInvokeTimeout(long invokeTimeout) {
		this.invokeTimeout = invokeTimeout;
	}
}
