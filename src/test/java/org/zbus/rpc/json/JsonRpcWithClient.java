package org.zbus.rpc.json;

import org.remoting.RemotingClient;
import org.zbus.client.rpc.json.JsonRpcProxy;


public class JsonRpcWithClient {
	
	public static void main(String[] args) throws Throwable { 
		RemotingClient client = new RemotingClient("127.0.0.1:15555");
		
		//2)通过JsonRpcProxy动态创建实现类
		ServiceInterface rpc = new JsonRpcProxy(client).getService(
				ServiceInterface.class, "mq=MyJsonRpc"); 
	 
		int c = rpc.plus(1, 2); 
		System.out.println(c);   
	}
}
