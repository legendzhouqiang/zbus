package org.zbus.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ZbusSpringPlugin {
	ApplicationContext applicationContext;
	public static String option(String[] args, String opt, String defaultValue){
		for(int i=0; i<args.length;i++){
			if(args[i].equals(opt)){
				if(i<args.length-1) return args[i+1];
			} 
		}
		return defaultValue;
	}
	
	public ZbusSpringPlugin(String config){
		this.applicationContext = new ClassPathXmlApplicationContext(config);
	}
	
	public static void main(String[] args) { 
		String config = option(args, "-conf", "plugins.xml");
		new ZbusSpringPlugin(config);
	}
}
