package org.zbus.mq;

import java.io.IOException;

import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;

public class Producer extends MqAdmin{  
	
	public Producer(Broker broker, String mq, MqMode... mode) {
		super(broker, mq, mode);
	} 
	
	public Producer(MqConfig config){
		super(config);
	}
	
	public void sendAsync(Message msg, final ResultCallback<Message> callback)
			throws IOException {
		msg.setCmd(Protocol.Produce);
		msg.setMq(this.mq); 
		msg.setAck(false);
		
		broker.invokeAsync(msg, callback);
	}
	
	public void sendAsync(Message msg) throws IOException {
		sendAsync(msg, null);
	}
	
	
	public Message sendSync(Message msg, int timeout) throws IOException{
		msg.setCmd(Protocol.Produce);
		msg.setMq(this.mq); 
		msg.setAck(true);
		
		return broker.invokeSync(msg, timeout);
	}
	
	public Message sendSync(Message msg) throws IOException{
		return sendSync(msg, 10000);
	}
	
}
