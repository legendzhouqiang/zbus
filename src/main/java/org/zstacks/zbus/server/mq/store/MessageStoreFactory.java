package org.zstacks.zbus.server.mq.store;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 

public class MessageStoreFactory {  
	private static final Logger log = LoggerFactory.getLogger(MessageStoreFactory.class);
	
	public static final String SQL   = "sql";  
	public static final String DUMMY = "dummy";  
	
    public static MessageStore getMessageStore(String borker, String type) {   
    	try{
	        if(SQL.equals(type)){ 
				log.info("Using SQL store");
				MessageStore store = new MessageStoreSql(borker);
				store.start();
				return store;
	        }else{
	        	//根据名称判断是否有外置扩展实现,若有加载之
	        	String configName = type + ".properties";
	        	InputStream stream = MessageStoreFactory.class.getClassLoader().getResourceAsStream(configName);
	    		try {
	    			if(stream != null){
	    				final Properties props = new Properties();
	    				props.load(stream);
	    				String configClass =  props.getProperty("class");
	    				if(configClass != null){
	    					Object temp = null;
	    					try {
	    						Class<?> c = Class.forName(configClass);
	    						Constructor<?> constructor=c.getDeclaredConstructor(String.class);   
	    						constructor.setAccessible(true);   
	    						temp = constructor.newInstance(borker);
	    						if (temp instanceof MessageStore){
		    						//正常创建了MessageStore对象，则返回。
		    						log.info("Using "+type+" store");
		    						return (MessageStore)temp;
		    					}else{
		    						log.warn("Class:"+ configClass + " must implements the MessageStore interface");
		    						log.warn("default to dummy store");
		    					}
	    					} catch (ClassNotFoundException e) {
	    						log.warn("Class:" + configClass + " can not found!", e);
	    						log.warn("default to dummy store");
	    					}catch(NoSuchMethodException e){
	    						log.warn("Class:" + configClass + " must have a constructor method with String parameter like this: ***MessageStoreImpl(String broker)" , e);
	    						log.warn("default to dummy store");
	    					}catch(Exception e){
	    						log.warn("Can not create instance of class: " + configClass , e);
	    						log.warn("default to dummy store");
	    					}
	    				}else{
	    					log.warn("Please set class=xxx.xxx.***MessageStoreImpl in " + configName);
	    					log.warn("default to dummy store");
	    				}
	    			}else{
	    				log.warn("default to dummy store");
	    			}
	    		} catch (IOException e) {
	    			log.error(e.getMessage(), e);
	    			log.warn("default to dummy store");
	    		}
	        }
	        
    	} catch (Exception e){
    		log.error(e.getMessage(), e);
    		log.warn("default to dummy store");
    	}
    	
        return new MessageStoreDummy();
    }
}
