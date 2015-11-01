/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.mq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Protocol {  
	public static final String Produce   = "produce";  //生产消息
	public static final String Consume   = "consume";  //消费消息  
	public static final String Route   	 = "route";  //请求等待应答消息  
	public static final String CreateMQ  = "create_mq"; //创建队列
	public static final String Auth      = "auth"; 
	public static final String Query     = "query"; 
	public static final String Test      = "test"; 
	
	
	public static final String Data      = "data"; 
	public static final String Jquery    = "jquery"; 
	
	
	public static enum MqMode {
		MQ,       //消息队列
		PubSub,   //发布订阅 
		Memory,   //是否临时
		RPC;
		
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
		public int consumerCount;
		public long unconsumedMsgCount;
		public List<ConsumerInfo> consumerInfoList = new ArrayList<ConsumerInfo>();
	}
	
	public static class ConsumerInfo {
		public String remoteAddr;
		public String status;
		public Set<String> topics;
	}
}
