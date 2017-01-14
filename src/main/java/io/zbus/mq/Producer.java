package io.zbus.mq;

import java.io.IOException;

public class Producer extends MqAdmin { 
	protected final Broker broker;
	public Producer(Broker broker, String topic) { 
		this.broker = broker;
		this.topic = topic;
	}

	public Producer(MqConfig config) {
		this.broker = config.getBroker();
		this.topic = config.getTopic();
		this.flag = config.getFlag();
		this.appid = config.getAppid();
		this.token = config.getToken();
		this.invokeTimeout = config.getInvokeTimeout();
	}   
	
	public Message publish(Message msg, int timeout) throws IOException, InterruptedException {
		fillCommonHeaders(msg);
		msg.setCommand(Protocol.Produce);
		
		return invokeSync(msg, timeout);
	}
 
	public void publishAsync(Message msg, final MessageCallback callback) throws IOException {
		fillCommonHeaders(msg);
		msg.setCommand(Protocol.Produce);
		invokeAsync(msg, callback);
	} 
	
	public Message publish(Message msg) throws IOException, InterruptedException {
		return publish(msg, invokeTimeout);
	} 
	
	public void publishAsync(Message msg) throws IOException {
		publishAsync(msg, null);
	}  
	
	@Override
	protected Message invokeSync(Message req, int timeout) throws IOException, InterruptedException {
		MessageInvoker invoker = null;
		try{
			String topic = req.getTopic();
			invoker = broker.selectForProducer(topic);
			if(invoker == null){
				throw new MqException("Missing MessageInvoker for Topic:"+topic);
			}
			return invoker.invokeSync(req, timeout);
			
		} finally {
			if(invoker != null){
				broker.releaseInvoker(invoker);
			}
		}
	}
	
	@Override
	protected void invokeAsync(Message req, MessageCallback callback) throws IOException {
		MessageInvoker invoker = null;
		try{
			String topic = req.getTopic();
			invoker = broker.selectForProducer(topic);
			if(invoker == null){
				throw new MqException("Missing MessageInvoker for Topic: "+topic);
			}
		    invoker.invokeAsync(req, callback);
		} finally {
			if(invoker != null){
				broker.releaseInvoker(invoker);
			}
		}
	}
}
