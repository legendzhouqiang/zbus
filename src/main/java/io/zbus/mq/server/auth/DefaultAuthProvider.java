package io.zbus.mq.server.auth;

import static io.zbus.kit.ConfigKit.valueOf;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

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
import io.zbus.kit.StrKit;
import io.zbus.mq.Message;
import io.zbus.mq.Protocol;

public class DefaultAuthProvider implements AuthProvider {  
	private TokenTable tokenTable = new TokenTable();
	
	@Override
	public boolean auth(Message message) {   
		if(!tokenTable.isEnabled()){ 
			return true;
		}
		String tokenStr = message.getToken();
		if(StrKit.isEmpty(tokenStr)) return false; //missing token 
		Token token = tokenTable.get(tokenStr);
		if(token == null) {
			return false;
		}
		if(token.allowAll) return true;
		
		String topic = message.getTopic();
		if(!token.topics.contains(topic)){ //topic not in token's list
			return false;
		} 
		String group = message.getConsumeGroup(); 
		if(!StrKit.isEmpty(group)){
			Set<String> groups = token.consumeGroups.get(topic);
			if(groups != null){
				if(!groups.contains(group)) return false;
			}
		}
		//now check ACL
		if(Operation.isEnabled(token.acl, Operation.ADMIN)){
			return true;
		}
		
		String cmd = message.getCommand();
		if(Protocol.PRODUCE.equals(cmd)){
			if(Operation.isEnabled(token.acl, Operation.PRODUCE)){
				return true;
			}
		}
		
		if(Protocol.CONSUME.equals(cmd) || Protocol.UNCONSUME.equals(cmd)){
			if(Operation.isEnabled(token.acl, Operation.CONSUME)){
				return true;
			}
		}
		
		if(Protocol.DECLARE.equals(cmd)){
			if(Operation.isEnabled(token.acl, Operation.DECLARE)){
				return true;
			}
		} 
		
		return false;
	}   
	
	public void loadFromXml(InputStream stream) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();     
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource source = new InputSource(stream); 
		Document doc = db.parse(source); 
		
		boolean enabled = valueOf(xpath.evaluate("/zbus/auth/@enabled", doc), false);
		tokenTable.setEnabled(enabled);
		 
		NodeList entryList = (NodeList) xpath.compile("/zbus/auth/*").evaluate(doc, XPathConstants.NODESET);
		if(entryList != null && entryList.getLength()> 0){ 
			for (int i = 0; i < entryList.getLength(); i++) {
			    Node tokenNode = entryList.item(i);    
			    Token token = new Token();
			    String tokenValue = valueOf(xpath.evaluate("@value", tokenNode), ""); 
			    if (tokenValue.equals("")) continue; //ignore 
			    token.token = tokenValue;
			    String acl = valueOf(xpath.evaluate("@acl", tokenNode), ""); 
			    token.setAcl(acl);
			    
			    NodeList topicList = (NodeList) xpath.compile("./*").evaluate(tokenNode, XPathConstants.NODESET);
			    for (int j = 0; j < topicList.getLength(); j++) {
				    Node topicNode = topicList.item(j);     
				    String topic = valueOf(xpath.evaluate("@value", topicNode), ""); 
				    if(topic.equals("*")){
				    	token.allowAll = true;
				    	continue;
				    }  
				    Set<String> consumeGroups = new HashSet<String>(); 
				    NodeList groupList = (NodeList) xpath.compile("./*").evaluate(topicNode, XPathConstants.NODESET);
				    for (int k = 0; k < groupList.getLength(); k++) {
				    	Node groupNode = groupList.item(k);     
					    String group = valueOf(xpath.evaluate("text()", groupNode), ""); 
					    if(group.equals("")) continue;
					    consumeGroups.add(group);
				    }
				    token.consumeGroups.put(topic, consumeGroups);
			    }  
			    this.tokenTable.put(token.token, token);
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
