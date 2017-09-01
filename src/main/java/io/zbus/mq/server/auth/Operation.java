package io.zbus.mq.server.auth;
 

public enum Operation { 
	ADMIN,
	PRODUCE,
	CONSUME,
	ROUTE,
	DECLARE, 
	EMPTY,
	REMOVE; 
	
	private final int mask;
	
	private Operation(){
        mask = (1 << ordinal());
    } 

    public final int getMask() {
        return mask;
    }
    
    public int intValue(){
    	return this.mask;
    }
    
    public static boolean isEnabled(int features, Operation feature) {
        return (features & feature.getMask()) != 0;
    }
    
    public static int intValue(Operation... features){
    	int value = 0;
    	for(Operation feature : features){
    		value |= feature.mask;
    	}
    	return value;
    }
}
