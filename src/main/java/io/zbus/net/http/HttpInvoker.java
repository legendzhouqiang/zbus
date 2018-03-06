package io.zbus.net.http;

import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.net.EventLoop; 

public class HttpInvoker {
	private static final Logger logger = LoggerFactory.getLogger(HttpInvoker.class);
	public static final EventLoop EVENT_LOOP = new EventLoop();
	

	public static String string(HttpMsg request, long timeoutInMillis) throws IOException {
		checkRequest(request);
		URI uri = request.getUri();
		String url = uri.toString() + request.getUrl();
		url = "Request(" + url + ")"; 
		HttpClient client = new HttpClient(uri, EVENT_LOOP);
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
	
	private static void checkRequest(HttpMsg req){
		if(req.getUri() == null) {
			throw new IllegalArgumentException("Missing uri for http request:\n" + req);
		}
	}
}
