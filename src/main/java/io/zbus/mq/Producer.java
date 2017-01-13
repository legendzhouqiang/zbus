package io.zbus.mq;

import java.io.IOException;

public class Producer extends MqAdmin {

	public Producer(Broker broker, String mq) {
		super(broker, mq);
	}

	public Producer(MqConfig config) {
		super(config);
	}  
 
	public void sendAsync(Message msg, final MessageCallback callback) throws IOException {
		fillCommonHeaders(msg);
		msg.setCmd(Protocol.Produce);
		broker.invokeAsync(msg, callback);
	}
 
	public void sendAsync(Message msg) throws IOException {
		sendAsync(msg, null);
	}
 
	public Message sendSync(Message msg, int timeout) throws IOException, InterruptedException {
		fillCommonHeaders(msg);
		msg.setCmd(Protocol.Produce);
		
		return broker.invokeSync(msg, timeout);
	}
 
	public Message sendSync(Message msg) throws IOException, InterruptedException {
		return sendSync(msg, invokeTimeout);
	} 
}
