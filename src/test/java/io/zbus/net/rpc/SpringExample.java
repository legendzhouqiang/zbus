package io.zbus.net.rpc;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringExample {
 
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		new ClassPathXmlApplicationContext("context.xml");      
	}

}
