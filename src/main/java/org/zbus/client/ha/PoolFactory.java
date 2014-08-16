package org.zbus.client.ha;

import org.zbus.client.ClientPool;
import org.zbus.remoting.ClientDispachterManager;

public class PoolFactory{
	public static final boolean IS_COMMONS_POOL2_AVAILABLE;  
    
    static {    
        IS_COMMONS_POOL2_AVAILABLE = isAvailable("org.apache.commons.pool2.ObjectPool");
    }

    public static String poolType() {  
    	if(IS_COMMONS_POOL2_AVAILABLE) return "commons-pool2"; 
        return "simple";
    }

    protected static boolean isAvailable(String classname) {
        try {
            return Class.forName(classname) != null;
        }
        catch(ClassNotFoundException cnfe) {
            return false;
        }
    }  
    
    public static ClientPool createPool(PoolConfig config, String broker, ClientDispachterManager manager){
    	if(IS_COMMONS_POOL2_AVAILABLE){
    		return new CommonsClientPool(config, broker, manager); 
    	}
    	
    	throw new IllegalStateException("no supported pool type"); 
    } 
}
