package io.zbus.rpc.http;

import java.io.IOException;

import com.alibaba.fastjson.JSON;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.net.ServerAdaptor;
import io.zbus.net.Session;
import io.zbus.net.http.HttpMsg;
import io.zbus.rpc.Request;
import io.zbus.rpc.Response;
import io.zbus.rpc.RpcProcessor;

public class HttpRpcServerAdaptor extends ServerAdaptor { 
	protected final RpcProcessor processor;  
	
	public HttpRpcServerAdaptor(RpcProcessor processor) {
		this.processor = processor;
	}
	
	@Override
	public void onMessage(Object msg, Session sess) throws IOException { 
		HttpMsg reqMsg = (HttpMsg)msg;
		Request request = handleUrlMessage(reqMsg);
		if(request == null){
			request = JSON.parseObject(reqMsg.getBodyString(), Request.class);
		}
		Response response = processor.process(request);
		String resStr = JSON.toJSONString(response);
		HttpMsg resMsg = new HttpMsg();
		resMsg.setStatus(200);
		resMsg.setBody(resStr);
		sess.write(resMsg);
	} 
	
	protected Request handleUrlMessage(HttpMsg msg) {
		String url = msg.getUrl(); 
    	if(url == null || "/".equals(url)){ 
    		return null;
    	} 
    	if(msg.getBody() != null) return null;
    	
		UrlInfo info = HttpKit.parseUrl(url);     
		
		Request req = new Request();
    	if(info.path.size()>=1){
    		req.module = info.path.get(0);
    	}
    	if(info.path.size()>=2){
    		req.method = info.path.get(1);
    	} 
    	
    	if(info.path.size()>2){
    		Object[] params = new Object[info.path.size()-2];
    		for(int i=0;i<params.length;i++){
    			params[i] = info.path.get(2+i);
    		}
    		req.params = params; 
    	}   
    	return req;
	}
}
