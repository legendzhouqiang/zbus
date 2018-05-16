package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.rpc.biz.User;

public class RpcClientExample {

	public static void main(String[] args) throws Exception { 
		RpcClient rpc = new RpcClient("localhost"); 

		Request req = new Request();
		req.setModule("example");
		req.setMethod("getOrder");  
		Response res = rpc.invoke(req); //同步调用
		System.out.println(res);
		
		rpc.invoke(req, resp -> { //异步调用
			System.out.println(resp); 
		}); 
		
		//动态代理类
		InterfaceExample example = rpc.createProxy(InterfaceExample.class, "example");
		User[] users = example.getUsers();
		System.out.println(users);
		 
		rpc.close(); 
	}
}
