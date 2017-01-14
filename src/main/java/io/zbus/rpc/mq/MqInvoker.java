package io.zbus.rpc.mq;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.Message;
import io.zbus.mq.MessageCallback;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.MqConfig;
import io.zbus.mq.Producer;

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
		return this.producer.publish(req, timeout);
	}

	@Override
	public void invokeAsync(Message req, MessageCallback callback) throws IOException {
		req.setAck(false);
		this.producer.publishAsync(req, callback);
	} 
	
	@Override
	public void close() throws IOException { 
		//nothing to do
	}
}
