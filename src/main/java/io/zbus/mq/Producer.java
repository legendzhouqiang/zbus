package io.zbus.mq;

import java.io.IOException;

import io.zbus.net.Sync.ResultCallback;
import io.zbus.net.http.Message;

public class Producer extends MqAdmin {

	public Producer(Broker broker, String mq) {
		super(broker, mq);
	}

	public Producer(MqConfig config) {
		super(config);
	} 
 
	public void produceAsync(Message msg, final ResultCallback<Message> callback) throws IOException {
		fillCommonHeaders(msg);
		msg.setCmd(Protocol.Produce);
		broker.invokeAsync(msg, callback);
	}
 
	public void produceAsync(Message msg) throws IOException {
		produceAsync(msg, null);
	}
 
	public Message produce(Message msg, int timeout) throws IOException, InterruptedException {
		fillCommonHeaders(msg);
		msg.setCmd(Protocol.Produce);
		
		return broker.invokeSync(msg, timeout);
	}
 
	public Message produce(Message msg) throws IOException, InterruptedException {
		return produce(msg, 10000);
	} 
}
