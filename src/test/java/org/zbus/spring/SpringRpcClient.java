package org.zbus.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.zbus.rpc.biz.Interface;

public class SpringRpcClient { 
	
	public static void main(String[] args) { 
		ApplicationContext context = new ClassPathXmlApplicationContext("SpringRpcClient.xml");
		 
		Interface intf = (Interface) context.getBean("interface"); 
		for(int i=0;i<100;i++){
			System.out.println(intf.listMap());
		} 
	} 
}
