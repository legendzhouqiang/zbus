package io.zbus.rpc.mq;

import java.io.IOException;

import io.zbus.mq.Message;
import io.zbus.mq.MessageCallback;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.Producer;
import io.zbus.mq.ProducerConfig;


public class MqInvoker implements MessageInvoker {
	private final Producer producer;
	private final String topic; 
	
	public MqInvoker(Producer producer, String topic) {
		this.producer = producer;
		this.topic = topic;
	}

	public MqInvoker(ProducerConfig config, String topic) {
		this.producer = new Producer(config);
		this.topic = topic;
	} 

	@Override
	public Message invokeSync(Message req, int timeout) throws IOException, InterruptedException {
		req.setAck(false);
		req.setTopic(this.topic);
		return this.producer.publish(req, timeout);
	}

	@Override
	public void invokeAsync(Message req, MessageCallback callback) throws IOException {
		req.setAck(false);
		req.setTopic(this.topic);
		
		this.producer.publishAsync(req, callback);
	} 
	
	@Override
	public void close() throws IOException { 
		//nothing to do
	}
}
