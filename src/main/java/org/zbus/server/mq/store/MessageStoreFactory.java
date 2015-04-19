package org.zbus.server.mq.store;

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
	        } 
	        
    	} catch (Exception e){
    		log.error(e.getMessage(), e);
    		log.warn("default to dummy store");
    	}
    	
        return new MessageStoreDummy();
    }
}
