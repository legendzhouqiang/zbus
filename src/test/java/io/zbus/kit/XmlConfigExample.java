package io.zbus.kit;

import io.zbus.mq.BrokerConfig;

public class XmlConfigExample {

	public static void main(String[] args) throws Exception {  
		BrokerConfig config = new BrokerConfig();
		config.loadFromXml("conf/broker.xml"); 
		
		System.out.println(config);
 
	}

}
