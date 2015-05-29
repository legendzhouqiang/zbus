package org.zstacks.zbus.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.zstacks.zbus.rpc.biz.Interface;

public class ZbusSpringClient {
	@Autowired
	Interface interface1;
	
	public static void main(String[] args) { 
		ApplicationContext context = new ClassPathXmlApplicationContext("ZbusSpringClient.xml");
		
		ZbusSpringClient client = (ZbusSpringClient) context.getBean("client");
		//Interface intf = (Interface) context.getBean("interface");
		Interface intf = client.interface1;
		System.out.println(intf.listMap());
	}

}
