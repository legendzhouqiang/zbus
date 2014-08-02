package org.zbus.common;


public enum MessageMode {
	MQ,     //消息队列
	PubSub, //发布订阅 
	Temp;   //是否临时
	
	private MessageMode(){
        mask = (1L << ordinal());
    }
	
    private final long mask;

    public final long getMask() {
        return mask;
    }
    
    public static boolean isEnabled(long features, MessageMode feature) {
        return (features & feature.getMask()) != 0;
    }
    
    public static int intValue(MessageMode... features){
    	int value = 0;
    	for(MessageMode feature : features){
    		value |= feature.mask;
    	}
    	return value;
    }
}