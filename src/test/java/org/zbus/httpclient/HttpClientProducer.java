package org.zbus.httpclient;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.zbus.mq.Protocol;
import org.zbus.net.http.Message;

public class HttpClientProducer {
	
	public static void main(String[] args) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		HttpPost http = new HttpPost("http://localhost:15555/"); 

		//zbus扩展HTTP头部协议，主要两个MQ和命令CMD
		http.addHeader(Message.CMD, Protocol.Produce);
		http.addHeader(Message.MQ, "MyMQ"); 
		
		http.setEntity(new StringEntity("hello world"));
		
		
		CloseableHttpResponse resp = httpClient.execute(http);
		HttpKit.printResponse(resp);
		resp.close();
		
		httpClient.close();
	}
	
}
