package io.zbus.rpc;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcServerSpringExample {
 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		new ClassPathXmlApplicationContext("rpc/context.xml");      
	}

}
