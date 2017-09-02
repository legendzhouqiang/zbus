package io.zbus.mq.server;


import static io.zbus.kit.ConfigKit.valueOf;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.zbus.kit.ConfigKit.XmlConfig;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class SslConfig extends XmlConfig {  
	private static final Logger log = LoggerFactory.getLogger(XmlConfig.class); 
	
	public boolean enabled = false; 
	public String storePath;
	public String defaultCertFile;
	public String serverCertFile;
	public String serverKeyFile;
	 
	public Map<String, String> certFileTable = new HashMap<String, String>(); 

	public void loadFromXml(Document doc) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();    
		
		this.enabled = valueOf(xpath.evaluate("/zbus/ssl/@enabled", doc), false);
		this.storePath = valueOf(xpath.evaluate("/zbus/ssl/storePath", doc), null);
		this.defaultCertFile = valueOf(xpath.evaluate("/zbus/ssl/defaultCertFile", doc), null); 
		this.serverCertFile = valueOf(xpath.evaluate("/zbus/ssl/serverCertFile", doc), null);
		this.serverKeyFile = valueOf(xpath.evaluate("/zbus/ssl/serverKeyFile", doc), null); 
		this.certFileTable.clear();
		
		NodeList list = (NodeList) xpath.compile("/zbus/ssl/certFileTable/*").evaluate(doc, XPathConstants.NODESET);
		if(list != null && list.getLength()> 0){ 
			for (int i = 0; i < list.getLength(); i++) {
			    Node node = list.item(i);    
			    String server = valueOf(xpath.evaluate("@server", node), null);
			    String certFile = valueOf(xpath.evaluate("@certFile", node), null); 
			    if(server == null || certFile == null){
			    	log.warn("certFileTable entry invalid");
			    	continue;
			    } 
			    this.certFileTable.put(server, certFile);
			}
		}   
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getStorePath() {
		return storePath;
	}

	public void setStorePath(String storePath) {
		this.storePath = storePath;
	}

	public String getDefaultCertFile() {
		return defaultCertFile;
	}

	public void setDefaultCertFile(String defaultCertFile) {
		this.defaultCertFile = defaultCertFile;
	}

	public String getServerCertFile() {
		return serverCertFile;
	}

	public void setServerCertFile(String serverCertFile) {
		this.serverCertFile = serverCertFile;
	}

	public String getServerKeyFile() {
		return serverKeyFile;
	}

	public void setServerKeyFile(String serverKeyFile) {
		this.serverKeyFile = serverKeyFile;
	}

	public Map<String, String> getCertFileTable() {
		return certFileTable;
	}

	public void setCertFileTable(Map<String, String> certFileTable) {
		this.certFileTable = certFileTable;
	} 
	
}