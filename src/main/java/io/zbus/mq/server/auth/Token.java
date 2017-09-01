package io.zbus.mq.server.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class Token {
	private static final Logger log = LoggerFactory.getLogger(Token.class); 
	
	public static class TopicResource {
		public String topic;
		public boolean allGroups = false;
		public Set<String> consumeGroups = new HashSet<String>(); 
	}
	
	public String token;
	public int acl;
	public boolean allTopics = false; 
	public Map<String, TopicResource> topics = new HashMap<String, TopicResource>();

	public void setAcl(String acl){
		this.acl = 0;
		String[] bb = acl.split("[,; ]");
		for(String opstr : bb){
			opstr = opstr.trim();
			if(opstr.equals("")) continue;
			
			try{
				Operation op = Operation.valueOf(opstr.toUpperCase());
				this.acl |= op.intValue();
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
				//ignore
			} 
		}
	} 
}
