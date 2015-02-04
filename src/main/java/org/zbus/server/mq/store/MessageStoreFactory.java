package org.zbus.server.mq.store;

import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;

import redis.clients.jedis.Jedis;

 

public class MessageStoreFactory {  
	private static final Logger log = LoggerFactory.getLogger(MessageStoreFactory.class);
	
	public static final String SQL   = "sql"; 
	public static final String REDIS = "redis";  
	public static final String DUMMY = "dummy"; 
	public static final boolean IS_REDIS_AVAILABLE;  
	public static final boolean IS_HSQLDB_AVAILABLE; 
    
    static {  
    	IS_HSQLDB_AVAILABLE = isAvailable("org.hsqldb.jdbcDriver");
    	IS_REDIS_AVAILABLE = isAvailable("redis.clients.jedis.Jedis");
    }


    public static String storeType() {  
    	if(IS_REDIS_AVAILABLE) return REDIS;  
    	if(IS_HSQLDB_AVAILABLE) return SQL;  
        return DUMMY;
    }

    protected static boolean isAvailable(String classname) {
        try {
            return Class.forName(classname) != null;
        }
        catch(ClassNotFoundException cnfe) {
            return false;
        }
    }

    public static MessageStore getMessageStore(String type) {   
        if(REDIS.equals(type)){
        	if(IS_REDIS_AVAILABLE){ 
        		log.info("Using Redis store");
        		String brokerAddress = "127.0.0.1:15555";
        		String redisAddress = "localhost";
        		Jedis jedis = new Jedis(redisAddress);
        		MessageStore store = new MessageStoreRedis(jedis, brokerAddress);
        		return store;
        	}
        	log.info("Redis missing, default to dummy store");
        } else if(SQL.equals(type)){
        	if(IS_HSQLDB_AVAILABLE){ 
        		log.info("Using SQL store");
        		MessageStore store = new MessageStoreSql();
        		return store;
        	}
        	log.info("HSQLDB missing, default to dummy store");
        } else if(DUMMY.equals(type)){
        	log.info("Using dummy store");
        }
        return new MessageStoreDummy();
    }
}
