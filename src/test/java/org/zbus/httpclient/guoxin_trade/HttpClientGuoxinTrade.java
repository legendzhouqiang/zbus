package org.zbus.httpclient.guoxin_trade;

import java.util.Arrays;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.zbus.httpclient.HttpKit;
import org.zbus.mq.Protocol;
import org.zbus.net.http.Message;

import com.alibaba.fastjson.JSONObject;

public class HttpClientGuoxinTrade { 
	
	public static void main(String[] args) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		HttpPost http = new HttpPost("http://localhost:15555/"); 
		//zbus扩展HTTP头部协议，主要两个MQ和命令CMD
		http.addHeader(Message.CMD, Protocol.Produce);
		http.addHeader(Message.MQ, "Trade");  
		http.addHeader(Message.ACK, "false");//Rpc模式下需要设置ack为false, 等待的是service的回复
		
		encryptTest(httpClient, http);
		tradeTest(httpClient, http);
		
		httpClient.close();
	}
	
	public static void encryptTest(CloseableHttpClient httpClient, HttpPost http) throws Exception{

		JSONObject json = new JSONObject();
		json.put("method", "encrypt");
		json.put("params", Arrays.asList("KDE", "123456", "110000001804")); //params 数组
		
		StringEntity body = new StringEntity(json.toJSONString());
		http.setEntity(body);
		
		
		CloseableHttpResponse resp = httpClient.execute(http);
		HttpKit.printResponse(resp);
		resp.close();
		
	}
	
	public static void tradeTest(CloseableHttpClient httpClient,  HttpPost http) throws Exception{
		Request req = new Request();
		req.funcId = "421324";
		req.accessIp = "127.0.0.1";
		req.tradeNodeId = "9501";
		req.authId = "";
		req.custOrg = "1100";
		req.loginId = "110002377535";
		req.params.add("110002377535");
		
		JSONObject json = new JSONObject();
		json.put("method", "call");
		json.put("params", Arrays.asList(req.toString())); //params 数组
		
		StringEntity body = new StringEntity(json.toJSONString());
		http.setEntity(body); 
		
		CloseableHttpResponse resp = httpClient.execute(http);
		HttpKit.printResponse(resp);
		resp.close();
		
	}
	
}
