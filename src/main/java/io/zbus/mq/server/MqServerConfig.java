package io.zbus.mq.server;


import static io.zbus.kit.ConfigKit.valueOf;
import static io.zbus.kit.ConfigKit.xeval;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class MqServerConfig{  
	
	public String serverHost = "0.0.0.0";
	public int serverPort = 15555;   
	public String trackServerList = null;
	
	public String sslCertificateFile;
	public String sslPrivateKeyFile;
	
	public boolean verbose = false;
	public String storePath = "/tmp/zbus"; 
	public String serverMainIpOrder; 
	public String serverName = "MqServer";
	
	public long cleanMqInterval = 3000; 
	public long trackReportInterval = 5000; 
	
	
	public String getServerAddress(){
		return serverHost + ":" + serverPort;
	}

	public String getTrackServerList() {
		return trackServerList;
	}

	public void setTrackServerList(String trackServerList) {
		this.trackServerList = trackServerList;
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

	public String getServerMainIpOrder() {
		return serverMainIpOrder;
	}

	public void setServerMainIpOrder(String serverMainIpOrder) {
		this.serverMainIpOrder = serverMainIpOrder;
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

	public String getSslCertificateFile() {
		return sslCertificateFile;
	}

	public void setSslCertificateFile(String sslCertificateFile) {
		this.sslCertificateFile = sslCertificateFile;
	}

	public String getSslPrivateKeyFile() {
		return sslPrivateKeyFile;
	}

	public void setSslPrivateKeyFile(String sslPrivateKeyFile) {
		this.sslPrivateKeyFile = sslPrivateKeyFile;
	}
 
 
	public void loadFromXml(InputSource source) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();     
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(source); 
		
		String prefix = "//zbus"; 
		this.serverHost = valueOf(xeval(xpath, doc, prefix, "host"), "0.0.0.0");  
		this.serverName = valueOf(xeval(xpath, doc, prefix, "serverName"), null);  
		this.serverPort = valueOf(xeval(xpath, doc, prefix, "port"), 15555);
		this.storePath = valueOf(xeval(xpath, doc, prefix, "mqStore"), "./store");
		this.verbose = valueOf(xeval(xpath, doc, prefix, "verbose"), false); 
		this.serverMainIpOrder = valueOf(xeval(xpath, doc, prefix, "mainIpOrder"),null);
		this.trackServerList = valueOf(xeval(xpath, doc, prefix, "trackServerList"),null);
		
		this.sslCertificateFile = valueOf(xeval(xpath, doc, prefix, "sslCertificateFile"),null);
		this.sslPrivateKeyFile = valueOf(xeval(xpath, doc, prefix, "sslPrivateKeyFile"),null);
	}
	 
	public void loadFromXml(String xmlConfigSourceFile) throws Exception{ 
		InputSource source = new InputSource(xmlConfigSourceFile);  
		loadFromXml(source);
	} 
	
	public void loadFromXml(InputStream stream) throws Exception{ 
		InputSource source = new InputSource(stream);  
		loadFromXml(source);
	}  
}