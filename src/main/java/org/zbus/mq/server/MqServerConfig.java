/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.mq.server;


import static org.zbus.kit.ConfigKit.valueOf;
import static org.zbus.kit.ConfigKit.xeval;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.zbus.net.EventDriver;
import org.zbus.proxy.HttpDmzProxy.ProxyConfig;

public class MqServerConfig{ 
	public String trackServerList = null;
	
	public String serverHost = "0.0.0.0";
	public int serverPort = 15555;  
	public EventDriver eventDriver;
	public String sslCertificateFile;
	public String sslPrivateKeyFile;
	
	public boolean verbose = false;
	public String storePath = "/tmp/zbus/mq";
	public String registerToken = "";  
	public String serverMainIpOrder;
	public boolean mqFilterPersist = false;
	public String serverName = "ZbusServer";
	public long cleanMqInterval = 3000; 
	public long trackReportInterval = 5000;
	public List<ProxyConfig> httpProxyConfigList;
	
	
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
	
	public EventDriver getEventDriver() {
		return eventDriver;
	}

	public void setEventDriver(EventDriver eventDriver) {
		this.eventDriver = eventDriver;
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

	public String getRegisterToken() {
		return registerToken;
	}

	public void setRegisterToken(String registerToken) {
		this.registerToken = registerToken;
	}

	public String getServerMainIpOrder() {
		return serverMainIpOrder;
	}

	public void setServerMainIpOrder(String serverMainIpOrder) {
		this.serverMainIpOrder = serverMainIpOrder;
	}

	public boolean isMqFilterPersist() {
		return mqFilterPersist;
	}

	public void setMqFilterPersist(boolean mqFilterPersist) {
		this.mqFilterPersist = mqFilterPersist;
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

	public List<ProxyConfig> getHttpProxyConfigList() {
		return httpProxyConfigList;
	}

	public void setHttpProxyConfigList(List<ProxyConfig> httpProxyConfigList) {
		this.httpProxyConfigList = httpProxyConfigList;
	}
 
	
	 
	public void loadFromXml(String xmlConfigSourceFile) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();   
		InputSource source = new InputSource(xmlConfigSourceFile);  
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(source); 
		
		String prefix = "//server"; 
		this.serverHost = valueOf(xeval(xpath, doc, prefix, "host"), "0.0.0.0");  
		this.serverPort = valueOf(xeval(xpath, doc, prefix, "port"), 15555);
		this.storePath = valueOf(xeval(xpath, doc, prefix, "mqStore"), "./store");
		this.verbose = valueOf(xeval(xpath, doc, prefix, "verbose"), false);
		this.registerToken = valueOf(xeval(xpath, doc, prefix, "registerToken"), "");
		this.mqFilterPersist = valueOf(xeval(xpath, doc, prefix, "mqFilter"), false);
		this.serverMainIpOrder = valueOf(xeval(xpath, doc, prefix, "mainIpOrder"),null);
		this.trackServerList = valueOf(xeval(xpath, doc, prefix, "trackServerList"),null);
		
		this.sslCertificateFile = valueOf(xeval(xpath, doc, prefix, "sslCertificateFile"),null);
		this.sslPrivateKeyFile = valueOf(xeval(xpath, doc, prefix, "sslPrivateKeyFile"),null);
		
		XPathExpression expr = xpath.compile("//http-proxy/*");
		NodeList list = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		if(list != null && list.getLength()> 0){
			this.httpProxyConfigList = new ArrayList<ProxyConfig>();
			for (int i = 0; i < list.getLength(); i++) {
			    Node node = list.item(i); 
			    ProxyConfig config = new ProxyConfig();
			    config.entry = (String) xpath.evaluate("name", node, XPathConstants.STRING);
			    config.target = (String) xpath.evaluate("target", node, XPathConstants.STRING);
			    this.httpProxyConfigList.add(config);
			}
		} 
	} 
}