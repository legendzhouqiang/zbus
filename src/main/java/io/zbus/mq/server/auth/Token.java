package io.zbus.mq.server.auth;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

/**
 * Token use Operation(Command) + Resource(Topic/ConsumeGroup) model
 * 
 * Topic and ConsumeGroup are case-insensitive
 * 
 * @author Rushmore
 *
 */
public class Token {
	private static final Logger log = LoggerFactory.getLogger(Token.class); 
	
	public static class TopicResource {
		public String topic;
		public boolean allGroups = false;
		public Set<String> consumeGroups = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	}
	
	public String token;
	//Operation
	public int operation;
	//Resource
	public boolean allTopics = false; 
	public Map<String, TopicResource> topics = new TreeMap<String, TopicResource>(String.CASE_INSENSITIVE_ORDER);

	public void setOperation(String op){
		this.operation = 0;
		String[] bb = op.split("[,; ]");
		for(String opstr : bb){
			opstr = opstr.trim();
			if(opstr.equals("")) continue;
			
			try{
				Operation operation = Operation.valueOf(opstr.toUpperCase());
				this.operation |= operation.intValue();
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
				//ignore
			} 
		}
	} 
}
