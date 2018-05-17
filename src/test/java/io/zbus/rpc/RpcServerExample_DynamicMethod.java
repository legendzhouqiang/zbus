package io.zbus.rpc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.zbus.rpc.http.ServiceBootstrap;


class GenericService implements GenericInvocation {

	@Override
	public Object invoke(String funcName, Map<String, Object> params) {
		Map<String, Object> res = new HashMap<>();
		res.put("invokedFunc", funcName);
		res.put("invokedParams", params);
		return res;
	}
	
}

public class RpcServerExample_DynamicMethod {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		ServiceBootstrap b = new ServiceBootstrap(); 
		b.setStackTraceEnabled(false); 
		
		GenericService service = new GenericService();
		
		//抽象的服务调用，增加一个具体的方法
		RpcMethod spec = new RpcMethod();
		spec.module = "test";
		spec.method = "func1";
		spec.paramTypes = Arrays.asList(String.class.getName(), Integer.class.getName());
		spec.paramNames = Arrays.asList("name", "age");
		spec.returnType = Map.class.getName();
		b.addMethod(spec, service);
		
		b.addModule("generic", service);
	
		b.setPort(80);
		b.start();
	}
}
