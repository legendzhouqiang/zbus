package io.zbus.rpc.mq;

import java.io.IOException;

import io.zbus.broker.Broker;
import io.zbus.mq.MqConfig;
import io.zbus.mq.Producer;
import io.zbus.net.Sync.ResultCallback;
import io.zbus.net.http.Message;
import io.zbus.net.http.Message.MessageInvoker;

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
