package io.zbus.mq;

import java.io.IOException;

public class Producer extends MqAdmin {

	public Producer(Broker broker, String topic) {
		super(broker, topic);
	}

	public Producer(MqConfig config) {
		super(config);
	}  
 
	public void publishAsync(Message msg, final MessageCallback callback) throws IOException {
		fillCommonHeaders(msg);
		msg.setCommand(Protocol.Produce);
		broker.invokeAsync(msg, callback);
	}
 
	public void publishAsync(Message msg) throws IOException {
		publishAsync(msg, null);
	}
 
	public Message publish(Message msg, int timeout) throws IOException, InterruptedException {
		fillCommonHeaders(msg);
		msg.setCommand(Protocol.Produce);
		
		return broker.invokeSync(msg, timeout);
	}
 
	public Message publish(Message msg) throws IOException, InterruptedException {
		return publish(msg, invokeTimeout);
	} 
}
