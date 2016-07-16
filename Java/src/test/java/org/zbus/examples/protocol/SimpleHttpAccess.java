package org.zbus.examples.protocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class SimpleHttpAccess {
	
	static void createMq(HttpPost http){
		http.addHeader("cmd", "create_mq");   
		http.addHeader("mq_name", "MyMQ1");   
	}
	
	static void removeMq(HttpPost http){
		http.addHeader("cmd", "remove_mq");   
		http.addHeader("mq_name", "MyMQ1");   
	}
	
	static void produce(HttpPost http) throws Exception{
		http.addHeader("cmd", "produce");   
		http.addHeader("mq", "MyMQ1");   
		http.setEntity(new StringEntity("hello world"));
	}
	
	static void consume(HttpPost http) throws Exception{
		http.addHeader("cmd", "consume");   
		http.addHeader("mq", "MyMQ1");    
	}
	
	static void rpcClient(HttpPost http) throws Exception{
		http.addHeader("cmd", "produce");  
		http.addHeader("ack", "false");//!!! ack set to false to wait for rpc result !!!
		http.addHeader("mq", "MyRpc");  
		
		JSONObject json = new JSONObject();
		json.put("method", "echo");
		json.put("params", Arrays.asList("zbus")); //params array
		StringEntity body = new StringEntity(json.toJSONString());
		http.setEntity(body); //body is the request json
	}

	static HttpPost urlRpcClient(String zbusAddress){
		Object[] params = new Object[]{"zbus"}; //parameter arrays, should be array!
		String jsonParamsString = JSON.toJSONString(params, SerializerFeature.UseSingleQuotes);
		
		String url = zbusAddress + "/MyRpc/echo?"+jsonParamsString; 
		return new HttpPost(url);
	}
	
	
	public static void main(String[] args) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault(); 
		String zbusAddress = "http://localhost:15555/";
		HttpPost http = new HttpPost(zbusAddress); 
		
		//createMq(http); 
		//removeMq(http);
		//produce(http);
		//consume(http); 
		//rpcClient(http);
		
		//Direct url to access 
		http = urlRpcClient(zbusAddress);
		
		CloseableHttpResponse resp = httpClient.execute(http);
		BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
		while (true) {
			String line = reader.readLine();
			if(line == null) break;
			System.out.println(line);
		}
		reader.close();
		resp.close(); 
		httpClient.close();
	}
	
}
