package org.zbus.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringRpcService {
	public static void main(String[] args) {  
		new ClassPathXmlApplicationContext("SpringRpcService.xml"); 
	}
}
