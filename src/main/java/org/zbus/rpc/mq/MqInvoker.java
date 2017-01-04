package org.zbus.rpc.mq;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.mq.MqConfig;
import org.zbus.mq.Producer;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;

/**
 * @author hong.leiming
 *
 */
public class MqInvoker implements MessageInvoker {
	private final Producer producer;

	public MqInvoker(Producer producer) {
		this.producer = producer;
	}

	public MqInvoker(Broker broker, String mq) {
		this.producer = new Producer(broker, mq);
	}
	
	public MqInvoker(MqConfig config) {
		this.producer = new Producer(config);
	}

	@Override
	public Message invokeSync(Message req, int timeout) throws IOException, InterruptedException {
		req.setAck(false);
		return this.producer.sendSync(req, timeout);
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback) throws IOException {
		req.setAck(false);
		this.producer.sendAsync(req, callback);
	}

	@Override
	public Message invokeSync(Message req) throws IOException, InterruptedException {
		req.setAck(false);
		return this.producer.sendSync(req);
	}
}
