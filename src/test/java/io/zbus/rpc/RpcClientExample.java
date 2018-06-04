package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.rpc.biz.User;

public class RpcClientExample {

	public static void main(String[] args) throws Exception {  
		RpcClient rpc = new RpcClient("localhost:8080"); 
		rpc.setAuthEnabled(true);
		rpc.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		rpc.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd");

		Map<String, Object> req = new HashMap<>();
		req.put("module", "example");
		req.put("method", "getOrder");
		
		Map<String, Object> res = rpc.invoke(req); //同步调用
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
