package io.zbus.mq;

import org.w3c.dom.Document;

import io.zbus.kit.ConfigKit.XmlConfig; 

public class MqServerConfig extends XmlConfig { 
	public int port = 15555;
	public int maxSocketCount = 102400;
	public int packageSizeLimit = 1024*1024*128; 
	
	
	@Override
	public void loadFromXml(Document doc) throws Exception { 
		
	}   
	
}
