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
    		log.warn("Missing persist.properties");
    		log.info("Using default MessageStoreDummy");
    		return new MessageStoreDummy(); 
    	}
    	
	    final Properties props = new Properties();
	    try {
			props.load(stream);
		} catch (IOException e) {  
			log.warn("Can not load persist.properties",e);
			log.info("Using default MessageStoreDummy");
			return new MessageStoreDummy();
		}
	    
	    String configClass =  props.getProperty("class");
	    if(configClass == null){ 
	    	log.warn("Missing class=xxx.MessateStoreImpl line in " + CONFIG_FILE + "");
	    	log.info("Using default MessageStoreDummy");
	    	return new MessageStoreDummy();
	    }
	    
	    Object temp = null; 
		try {
			Class<?> c = Class.forName(configClass);
			Constructor<?> constructor=c.getDeclaredConstructor(String.class, Properties.class);   
			constructor.setAccessible(true);   
			temp = constructor.newInstance(borker, props);
			if (temp instanceof MessageStore){ 
				log.info("Using MessageStore="+c.getSimpleName());
				return (MessageStore)temp;
			} else {
				log.warn("Class:"+ configClass + " must implements the MessageStore interface");
			}
		} catch (ClassNotFoundException e) {
			log.warn("Class:" + configClass + " can not found!", e);
		} catch (NoSuchMethodException e){
			log.warn("Class:" + configClass + " must have a constructor method like this: ***MessageStoreImpl(String broker, Properties props)" , e);
		} catch (Exception e){
			log.warn("Can not create instance of class: " + configClass , e);
		} 
		
		log.info("Using default MessageStoreDummy");
		return new MessageStoreDummy(); 
    }
}
