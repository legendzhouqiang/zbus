package org.zbus.mq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Protocol { 
	public static final String Heartbeat = "heartbeat";
	
	public static final String Produce   = "produce";         //生产消息
	public static final String Consume   = "consume";         //消费消息  
	public static final String Route   	 = "route";           //请求等待应答消息  
	public static final String CreateMQ  = "create_mq"; 
	
	
	public static final String Admin     = "admin";           //管理类消息  
	
	public static enum MqMode {
		MQ,       //消息队列
		PubSub,   //发布订阅 
		Memory;   //是否临时
		
		private MqMode(){
	        mask = (1 << ordinal());
	    }
		
	    private final int mask;

	    public final int getMask() {
	        return mask;
	    }
	    
	    public int intValue(){
	    	return this.mask;
	    }
	    
	    public static boolean isEnabled(int features, MqMode feature) {
	        return (features & feature.getMask()) != 0;
	    }
	    
	    public static int intValue(MqMode... features){
	    	int value = 0;
	    	for(MqMode feature : features){
	    		value |= feature.mask;
	    	}
	    	return value;
	    }
	}
	 
	public static class BrokerInfo{
		public String broker;
		public long lastUpdatedTime = System.currentTimeMillis(); 
		public Map<String, MqInfo> mqTable = new HashMap<String, MqInfo>(); 
		
		public boolean isObsolete(long timeout){
			return (System.currentTimeMillis()-lastUpdatedTime)>timeout;
		}
	}
	 
	public static class MqInfo { 
		public String name;
		public int mode;
		public String creator;
		public long lastUpdateTime;
		public long unconsumedMsgCount;
		public List<ConsumerInfo> consumerInfoList = new ArrayList<ConsumerInfo>();
	}
	
	public static class ConsumerInfo {
		public String remoteAddr;
		public String status;
		public Set<String> topics;
	}
}
