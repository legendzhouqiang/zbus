package org.zbus.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.zbus.client.Service;

public class ZbusSpring {

	public static void main(String[] args) { 
		ApplicationContext context = new ClassPathXmlApplicationContext("ZbusSpring.xml");
		
		Service service = (Service) context.getBean("zbusService");
		
		System.out.println(service); 
	}

}
