package io.zbus.mq;

import org.w3c.dom.Document;

import io.zbus.kit.ConfigKit.XmlConfig; 

public class MqServerConfig extends XmlConfig { 
	public Integer port;
	public int maxSocketCount = 102400;
	public int packageSizeLimit = 1024*1024*128; 
	public String mqDir = "/tmp/zbus";
	
	@Override
	public void loadFromXml(Document doc) throws Exception { 
		
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public int getMaxSocketCount() {
		return maxSocketCount;
	}

	public void setMaxSocketCount(int maxSocketCount) {
		this.maxSocketCount = maxSocketCount;
	}

	public int getPackageSizeLimit() {
		return packageSizeLimit;
	}

	public void setPackageSizeLimit(int packageSizeLimit) {
		this.packageSizeLimit = packageSizeLimit;
	}

	public String getMqDir() {
		return mqDir;
	}

	public void setMqDir(String mqDir) {
		this.mqDir = mqDir;
	}    
}
