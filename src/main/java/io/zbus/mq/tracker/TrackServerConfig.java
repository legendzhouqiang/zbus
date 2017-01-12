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
package io.zbus.mq.tracker;

import static io.zbus.util.ConfigUtil.valueOf;
import static io.zbus.util.ConfigUtil.xeval;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import io.zbus.net.EventDriver;

public class TrackServerConfig{  
	public String serverHost = "0.0.0.0";
	public int serverPort = 15555;  
	public EventDriver eventDriver;
	public String sslCertificateFile;
	public String sslPrivateKeyFile;
	
	public boolean verbose = false; 
	
	
	public String getServerAddress(){
		return serverHost + ":" + serverPort;
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
		
		String prefix = "//server"; 
		this.serverHost = valueOf(xeval(xpath, doc, prefix, "host"), "0.0.0.0");  
		this.serverPort = valueOf(xeval(xpath, doc, prefix, "port"), 16666); 
		this.verbose = valueOf(xeval(xpath, doc, prefix, "verbose"), true); 
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