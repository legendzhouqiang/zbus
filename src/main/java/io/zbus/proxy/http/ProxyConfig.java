package io.zbus.proxy.http;

import static io.zbus.kit.ConfigKit.valueOf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import io.zbus.kit.FileKit;
import io.zbus.mq.Broker;

public class ProxyConfig { 
	public Broker broker; 
	public String brokerAddress;
	public int connectionCount = 4; //Number of connections to zbus broker per consumer 
	public Map<String, ProxyEntry> entryTable = new HashMap<String, ProxyEntry>(); 

	public static class ProxyEntry {
		public String entry;
		public List<String> targetList = new ArrayList<String>();
	} 

	public Broker getBroker() {
		return broker;
	}

	public int getConnectionCount() {
		return connectionCount;
	} 
 
	public void setBroker(Broker broker) {
		this.broker = broker;
	}

	public void setConnectionCount(int connectionCount) {
		this.connectionCount = connectionCount;
	} 
	
	public String getBrokerAddress() {
		return brokerAddress;
	}

	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	} 

	public Map<String, ProxyEntry> getEntryTable() {
		return entryTable;
	}

	public void setEntryTable(Map<String, ProxyEntry> entryTable) {
		this.entryTable = entryTable;
	}

	public void loadFromXml(InputStream stream) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();     
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource source = new InputSource(stream); 
		Document doc = db.parse(source); 
		
		this.brokerAddress = valueOf(xpath.evaluate("/zbus/httpProxy/@zbus", doc), "localhost:15555");  
		this.connectionCount = valueOf(xpath.evaluate("/zbus/httpProxy/@connectionCount", doc), 4);   
		 
		NodeList entryList = (NodeList) xpath.compile("/zbus/httpProxy/*").evaluate(doc, XPathConstants.NODESET);
		if(entryList != null && entryList.getLength()> 0){ 
			for (int i = 0; i < entryList.getLength(); i++) {
			    Node node = entryList.item(i);    
			    ProxyEntry entry = new ProxyEntry();
			    String entryName = valueOf(xpath.evaluate("@entry", node), ""); 
			    if (entryName.equals("")) continue;
			    entry.entry = entryName;
			    
			    NodeList targetList = (NodeList) xpath.compile("./*").evaluate(node, XPathConstants.NODESET);
			    for (int j = 0; j < targetList.getLength(); j++) {
				    Node targetNode = targetList.item(j);    
				    String target = targetNode.getFirstChild().getNodeValue();
				    entry.targetList.add(target);
			    } 
			    
			    this.entryTable.put(entryName, entry);
			}
		}   
	}
	
	public void loadFromXml(String configFile) { 
		InputStream stream = FileKit.inputStream(configFile);
		if(stream == null){
			throw new IllegalArgumentException(configFile + " not found");
		}
		try { 
			loadFromXml(stream); 
		} catch (Exception e) { 
			throw new IllegalArgumentException(configFile + " load error", e);
		} finally {
			if(stream != null){
				try {
					stream.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
	} 
}