package io.zbus.mq.server;


import static io.zbus.kit.ConfigKit.valueOf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.zbus.kit.ConfigKit.XmlConfig;
import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.server.auth.AuthProvider;
import io.zbus.mq.server.auth.XmlAuthProvider;
import io.zbus.transport.ServerAddress;

public class MqServerConfig extends XmlConfig  {  
	private static final Logger log = LoggerFactory.getLogger(MqServerConfig.class); 
	
	public String serverHost = "0.0.0.0";
	public int serverPort = 15555;   
	public List<ServerAddress> trackerList = new ArrayList<ServerAddress>();  
	
	public boolean sslEnabled = false;  
	public String sslCertFile;
	public String sslKeyFile;
	
	public boolean trackerOnly = false;
	public boolean verbose = false;
	public String storePath = "/tmp/zbus";  
	public String serverName;
	
	public long cleanMqInterval = 3000;       //3 seconds
	public long trackReportInterval = 30000;  //30 seconds
	
	public AuthProvider authProvider = new XmlAuthProvider();  
	
	public boolean compatible = false;  //set protocol compatible to zbus7 if true
	
	public MqServerConfig(){ 
		
	}
	
	public MqServerConfig(String configXmlFile) {
		loadFromXml(configXmlFile);
	}
	
	public void loadFromXml(Document doc) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();     
		this.serverHost = valueOf(xpath.evaluate("/zbus/serverHost", doc), "0.0.0.0");   
		this.serverPort = valueOf(xpath.evaluate("/zbus/serverPort", doc), 15555);
		
		this.serverName = valueOf(xpath.evaluate("/zbus/serverName", doc), null); 
		this.storePath = valueOf(xpath.evaluate("/zbus/storePath", doc), "/tmp/zbus");
		this.verbose = valueOf(xpath.evaluate("/zbus/verbose", doc), false);  
		this.compatible = valueOf(xpath.evaluate("/zbus/compatible", doc), false);
		
		this.sslEnabled = valueOf(xpath.evaluate("/zbus/sslEnabled", doc), false);
		this.sslCertFile = valueOf(xpath.evaluate("/zbus/sslEnabled/@certFile", doc), null);
		this.sslKeyFile = valueOf(xpath.evaluate("/zbus/sslEnabled/@keyFile", doc), null);
		 
		NodeList list = (NodeList) xpath.compile("/zbus/trackerList/*").evaluate(doc, XPathConstants.NODESET);
		if(list != null && list.getLength()> 0){ 
			for (int i = 0; i < list.getLength(); i++) {
			    Node node = list.item(i);    
			    String address = xpath.evaluate("address", node);
			    String sslEnabled = xpath.evaluate("sslEnabled", node);   
			    String certFile = xpath.evaluate("sslEnabled/@certFile", node);  
			    String token = xpath.evaluate("token", node); 
			    if(StrKit.isEmpty(address)) continue; 
			    
			    ServerAddress trackerAddress = new ServerAddress(address, valueOf(sslEnabled, false));
			    trackerAddress.setToken(token);
			    if(trackerAddress.isSslEnabled()){
			    	trackerAddress.setCertFile(certFile);
			    } 
			    trackerList.add(trackerAddress); 
			}
		}   
		
		String authClass = valueOf(xpath.evaluate("/zbus/auth/@class", doc), "");
		if(authClass.equals("")){
			XmlAuthProvider provider = new XmlAuthProvider();
			provider.loadFromXml(doc);
			this.setAuthProvider(provider);
		} else {
			try{
				Class<?> clazz = Class.forName(authClass);
				Object auth = clazz.newInstance();
				if(auth instanceof AuthProvider){
					this.setAuthProvider((AuthProvider)auth);
				} else {
					log.warn("auth class is not AuthProvider type");
				}
			} catch (Exception e) { 
				log.error("Load AuthProvider error: " + e);
			}
		}
	} 
	
	public void addTracker(String trackerAddress, String certFile) throws IOException{
		ServerAddress address = new ServerAddress(trackerAddress);
		if(certFile != null){
			address.setSslEnabled(true);
			address.setCertFile(certFile);
		} 
		
		if(!trackerList.contains(address)){
			trackerList.add(address);
		} 
	}
	
	public void addTracker(String trackerAddress){
		try {
			addTracker(trackerAddress, null);
		} catch (IOException e) {
			// ignore
		}
	} 

	public String getServerHost() {
		return serverHost;
	}

	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	} 
	
	public List<ServerAddress> getTrackerList() {
		return trackerList;
	}

	public void setTrackerList(List<ServerAddress> trackerList) {
		this.trackerList = trackerList;
	}
	
	
	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}

	public String getSslCertFile() {
		return sslCertFile;
	}

	public void setSslCertFile(String sslCertFile) {
		this.sslCertFile = sslCertFile;
	}

	public String getSslKeyFile() {
		return sslKeyFile;
	}

	public void setSslKeyFile(String sslKeyFile) {
		this.sslKeyFile = sslKeyFile;
	}

	public boolean isTrackerOnly() {
		return trackerOnly;
	}

	public void setTrackerOnly(boolean trackerOnly) {
		this.trackerOnly = trackerOnly;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public String getStorePath() {
		return storePath;
	}

	public void setStorePath(String storePath) {
		this.storePath = storePath;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public long getCleanMqInterval() {
		return cleanMqInterval;
	}

	public void setCleanMqInterval(long cleanMqInterval) {
		this.cleanMqInterval = cleanMqInterval;
	}

	public long getTrackReportInterval() {
		return trackReportInterval;
	}

	public void setTrackReportInterval(long trackReportInterval) {
		this.trackReportInterval = trackReportInterval;
	}

	public AuthProvider getAuthProvider() {
		return authProvider;
	}

	public void setAuthProvider(AuthProvider authProvider) {
		this.authProvider = authProvider;
	}

	public boolean isCompatible() {
		return compatible;
	}

	public void setCompatible(boolean compatible) {
		this.compatible = compatible;
	}  
}