package org.zbus;

import org.zbus.client.ServiceConfig;
import org.zbus.client.ServiceHandler;
import org.zbus.client.container.ServiceProvider;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.Message;

public class MyServicePlugin implements ServiceProvider, ServiceHandler {
	private static final Logger log = LoggerFactory.getLogger(MyServicePlugin.class);
	
	@Override
	public ServiceConfig getConfig() { 
		ServiceConfig config = new ServiceConfig();
		config.setMq("Http");
		config.setThreadCount(2); 
		config.setServiceHandler(this);
		
		return config;
	}
	
	@Override
	public Message handleRequest(Message request) { 
		log.info(request.toString());
		return null;
	}
	
}
