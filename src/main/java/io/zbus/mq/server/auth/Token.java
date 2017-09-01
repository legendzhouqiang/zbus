package io.zbus.mq.server.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class Token {
	private static final Logger log = LoggerFactory.getLogger(Token.class); 
	
	public String token;
	public int acl;
	public boolean allowAll = false;
	public Set<String> topics = new HashSet<String>();
	public Map<String, Set<String>> consumeGroups = new HashMap<String, Set<String>>(); //{topic=>[groups]}
	
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
