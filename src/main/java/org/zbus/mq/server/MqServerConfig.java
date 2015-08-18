package org.zbus.mq.server;

import static org.zbus.kit.ConfigKit.value;
import static org.zbus.kit.ConfigKit.valueSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.zbus.kit.FileKit;
import org.zbus.log.Logger;

public class MqServerConfig{
	private static final Logger log = Logger.getLogger(MqServerConfig.class);
	
	public String serverHost = "0.0.0.0";
	public int serverPort = 15555; 
	public int selectorCount = 1;
	public int executorCount = 64;
	public boolean verbose = true;
	public String storePath = "mq";
	 
	
	public Set<String> roles = new HashSet<String>();
	public Set<User> users = new HashSet<User>();
	public Set<MqEntry> mqs = new HashSet<MqEntry>(); 
	public Set<MqEntry> pubsubs = new HashSet<MqEntry>(); 
	
	public void load(InputStream is) throws IOException{
		Properties props = new Properties();
		if(is == null){
			log.info("Using default settings, missing config file");
		} else {
			props.load(is);
		}
		
		this.serverHost = value(props, "zbus.serverHost", "0.0.0.0");
		this.serverPort = value(props, "zbus.serverPort", 15555);
		this.selectorCount = value(props, "zbus.selectorCount", 1);
		this.executorCount = value(props, "zbus.executorCount", 64);
		this.verbose = value(props, "zbus.verbose", true);
		this.storePath = value(props, "zbus.storePath", "mq");
		
		this.roles = valueSet(props, "zbus.role"); 
		Set<String> userNames  = valueSet(props, "zbus.user");
		for(String name : userNames){
			User user = new User();
			user.name = name;
			user.password = value(props, String.format("zbus.user.%s.password", name));
			user.roles = valueSet(props, String.format("zbus.user.%s.role", name));
			
			this.users.add(user);
		}
		 
		for(String name : valueSet(props, "zbus.mq")){
			MqEntry entry = new MqEntry();
			entry.name = name;
			entry.allowUsers = valueSet(props, String.format("zbus.mq.%s.allowUser", name));
			entry.allowRoles = valueSet(props, String.format("zbus.mq.%s.allowRole", name));
			
			this.mqs.add(entry);
		}  
		 
		for(String name : valueSet(props, "zbus.pubsub")){
			MqEntry entry = new MqEntry();
			entry.name = name;
			entry.allowUsers = valueSet(props, String.format("zbus.pubsub.%s.allowUser", name));
			entry.allowRoles = valueSet(props, String.format("zbus.pubsub.%s.allowRole", name));
			
			this.pubsubs.add(entry);
		}  
	}

	public void load(String fileName) throws IOException{ 
		try{
			InputStream is = FileKit.loadFile(fileName);
			if(is == null){
				log.info("Using default settings, missing config file");
			} else {
				load(is);
			} 
		} catch(Exception e){ 
			log.error(e.getMessage(), e);
		} 
	} 
	
	public static class User {
		public String name;
		public String password;
		public Set<String> roles = new HashSet<String>();
		@Override
		public String toString() {
			return "User [name=" + name + ", password=" + password + ", roles="
					+ roles + "]";
		} 
	}
	
	public static class MqEntry {
		public String name; 
		public Set<String> allowUsers = new HashSet<String>();
		public Set<String> allowRoles = new HashSet<String>();
		@Override
		public String toString() {
			return "MqEntry [name=" + name + ", allowUsers=" + allowUsers
					+ ", allowRoles=" + allowRoles + "]";
		}  
		
	}
	 
	 

	public static void main(String[] args) throws Exception{
		MqServerConfig config = new MqServerConfig();
		
		config.load("zbus.properties");
		
		System.out.println("roles:"+config.roles);
		System.out.println("users:"+config.users); 
		System.out.println("mqs:"+config.mqs); 
		System.out.println("pubsubs:"+config.pubsubs);  
	}
	
}