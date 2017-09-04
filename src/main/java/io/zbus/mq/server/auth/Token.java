package io.zbus.mq.server.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;

/**
 * Token use Operation(Command) + Resource(Topic/ConsumeGroup) model
 * 
 * Topic and ConsumeGroup are case-insensitive
 * 
 * @author Rushmore
 *
 */
public class Token { 
	
	public static class TopicResource {
		public String topic;
		public boolean allGroups = false;
		public Set<String> consumeGroups = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	}
	
	public final static Token ALLOW = new Token(true);
	public final static Token DENY = new Token(false);
	
	public String token;
	public boolean denyAll = false;
	//Operation
	public int operation;
	public boolean allOperations = false;
	
	//Resource 
	public Map<String, TopicResource> topics = new TreeMap<String, TopicResource>(String.CASE_INSENSITIVE_ORDER);
	public boolean allTopics = false; 
	
	public Token(){
		
	}
	
	public Token(boolean allowOrDenyAll){
		if(allowOrDenyAll){
			allOperations = true;
			allTopics = true;
		} else {
			denyAll = true;
		}
	} 
	
	private static final Logger log = LoggerFactory.getLogger(Token.class); 
	public void setOperation(String op){
		this.operation = 0;
		String[] bb = op.split("[,; ]");
		for(String cmd : bb){
			cmd = cmd.trim();
			if(cmd.equals("")) continue;
			
			try{
				Operation operation = Operation.find(cmd.toLowerCase());
				if(operation == null) continue;
				this.operation |= operation.getMask();
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
				//ignore
			} 
		}
	}  
	
	public ServerInfo filter(ServerInfo info){  
		if(Operation.isEnabled(operation, Operation.ADMIN)){
			return info;
		}
		
		if(this.allTopics){
			return info;
		}
		ServerInfo newInfo = info.clone();
		newInfo.topicTable = new HashMap<String, TopicInfo>(); 
		if(this.denyAll){  
			return newInfo;
		}  
		for(Entry<String, TopicInfo> e : info.topicTable.entrySet()){
			String topic = e.getKey(); 
			TopicInfo topicInfo = e.getValue();
			TopicResource topicResource = this.topics.get(topic);
			if(topicResource == null){ 
				continue;
			} 
			if(topicResource.allGroups){
				newInfo.topicTable.put(topic, topicInfo);
				continue;
			}
			
			TopicInfo newTopicInfo = topicInfo.clone();
			newTopicInfo.consumeGroupList = new ArrayList<ConsumeGroupInfo>();
			for(ConsumeGroupInfo groupInfo : topicInfo.consumeGroupList){
				if(topicResource.consumeGroups.contains(groupInfo.groupName)){
					newTopicInfo.consumeGroupList.add(groupInfo);
				}
			}
			newInfo.topicTable.put(topic, newTopicInfo); 
		} 
		return newInfo;
	}
}
