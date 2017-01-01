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
	public static final String Produce   = "produce";   //produce message
	public static final String Consume   = "consume";   //consume message
	public static final String Route   	 = "route";     //route back message to sender

	public static final String QueryMQ   = "query_mq"; 
	public static final String CreateMQ  = "create_mq"; //create MQ
	public static final String RemoveMQ  = "remove_mq"; //remove MQ  
	 
	public static final String Test      = "test"; 
	public static final String Data      = "data"; 
	public static final String Jquery    = "jquery"; 
	
	public static final int FlagMemory    = 1<<1; 
	public static final int FlagRpc    = 1<<4; 
	 
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
