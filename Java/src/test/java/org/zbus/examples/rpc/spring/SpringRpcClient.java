package org.zbus.examples.rpc.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.zbus.examples.rpc.appdomain.InterfaceExample;

public class SpringRpcClient { 
	
	public static void main(String[] args) { 
		ApplicationContext context = new ClassPathXmlApplicationContext("SpringRpcClient.xml");
		 
		InterfaceExample intf = (InterfaceExample) context.getBean("interface"); 
		
		System.out.println(intf.listMap());
		
	} 
}
