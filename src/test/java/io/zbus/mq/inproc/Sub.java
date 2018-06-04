package io.zbus.mq.inproc;

import io.zbus.mq.MqServer;
import io.zbus.mq.MqServerConfig;
import io.zbus.transport.inproc.InprocClient;

public class Sub { 
	
	@SuppressWarnings({ "resource", "unused" })
	public static void main(String[] args) throws Exception { 
		MqServer server = new MqServer(new MqServerConfig());
		InprocClient client = new InprocClient(server.getServerAdaptor());   
	} 
}
