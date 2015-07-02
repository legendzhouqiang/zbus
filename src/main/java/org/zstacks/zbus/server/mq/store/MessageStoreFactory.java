package org.zstacks.zbus.server.mq.store;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 

public class MessageStoreFactory {  
	private static final Logger log = LoggerFactory.getLogger(MessageStoreFactory.class);  
	public static final String CONFIG_FILE = "persist.properties";
	
    public static MessageStore getMessageStore(String borker) {   
    	InputStream stream = MessageStoreFactory.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
    	if(stream == null) {
    		log.info("Using MessageStore="+MessageStoreDummy.class.getSimpleName());
    		return new MessageStoreDummy();
    	}
    	
	    final Properties props = new Properties();
	    try {
			props.load(stream);
		} catch (IOException e) {  
			log.warn("Missing persist.properties, default to dummy store");
			return new MessageStoreDummy();
		}
	    
	    String configClass =  props.getProperty("class");
	    if(configClass == null){ 
	    	log.warn("Missing class=xxx.MessateStoreImpl line in " + CONFIG_FILE);
	    	return new MessageStoreDummy();
	    }
	    
	    Object temp = null; 
		try {
			Class<?> c = Class.forName(configClass);
			Constructor<?> constructor=c.getDeclaredConstructor(String.class, Properties.class);   
			constructor.setAccessible(true);   
			temp = constructor.newInstance(borker, props);
			if (temp instanceof MessageStore){ 
				log.info("Using MessageStore="+configClass);
				return (MessageStore)temp;
			} else {
				log.warn("Class:"+ configClass + " must implements the MessageStore interface");
				log.warn("default to dummy store");
			}
		} catch (ClassNotFoundException e) {
			log.warn("Class:" + configClass + " can not found!", e);
			log.warn("default to dummy store");
		} catch (NoSuchMethodException e){
			log.warn("Class:" + configClass + " must have a constructor method with String parameter like this: ***MessageStoreImpl(String broker, Properties props)" , e);
			log.warn("default to dummy store");
		} catch (Exception e){
			log.warn("Can not create instance of class: " + configClass , e);
			log.warn("default to dummy store");
		} 
		
		log.info("Using MessageStore="+MessageStoreDummy.class.getSimpleName());
		return new MessageStoreDummy(); 
    }
}
