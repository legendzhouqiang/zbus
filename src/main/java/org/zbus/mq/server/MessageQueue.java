
package org.zbus.mq.server;

import java.io.IOException;

import org.zbus.mq.Protocol.MqInfo;
import org.zbus.net.Session;
import org.zbus.net.http.Message;

public interface MessageQueue { 
	
	public class ConsumeGroupCtrl{
		public String groupName; //required
		public String baseGroupName;
		public Long consumeStartOffset;
		public Long consumeStartTime;
		public String consumeStartMsgId;//to validate consumeStartOffset 
		@Override
		public String toString() {
			return "ConsumeGroupCtrl [groupName=" + groupName + ", baseGroupName=" + baseGroupName
					+ ", consumeStartOffset=" + consumeStartOffset + ", consumeStartTime=" + consumeStartTime
					+ ", consumeStartMsgId=" + consumeStartMsgId + "]";
		} 
	}
	
	void produce(Message message, Session session) throws IOException;
	
	void consume(Message message, Session session) throws IOException;  
	
	void declareConsumeGroup(ConsumeGroupCtrl ctrl) throws Exception;
	
	int remaining(String consumeGroup);
	
	int consumerCount(String consumeGroup); 
	
	void cleanSession(Session sess);
	
	void cleanSession();
	
	MqInfo getMqInfo();
	
	String getCreator();

	void setCreator(String value);

	String getAccessToken();

	void setAccessToken(String value); 
	
	int getFlag();

	void setFlag(int value); 
	
	long getLastUpdateTime();
	
	String getName();
}