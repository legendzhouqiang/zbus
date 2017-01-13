package io.zbus.mq;

public class ConsumerServiceConfig extends MqConfig{
	private ConsumerHandler consumerHandler; 
	private MessageProcessor messageProcessor; 
	private int consumerCount = 4; 
	private boolean consumerHandlerRunInPool = true;
	private int consumerHandlerPoolSize = 64; 
	private int inFlightMessageCount = 100;
	private boolean verbose = false;
	private Broker[] brokers;
 
	public ConsumerServiceConfig(Broker... brokers) {
		this.brokers = brokers;
		if (brokers.length > 0) {
			setBroker(brokers[0]);
		} 
	}

	public void setBrokers(Broker[] brokers) {
		this.brokers = brokers;
		if (brokers.length > 0) {
			setBroker(brokers[0]);
		}
		
	}

	public Broker[] getBrokers() {
		if (brokers == null || brokers.length == 0) {
			if (getBroker() != null) {
				brokers = new Broker[] { getBroker() };
			}
		}
		return this.brokers;
	}

	public int getConsumerCount() {
		return consumerCount;
	}

	public void setConsumerCount(int consumerCount) {
		this.consumerCount = consumerCount;
	}

	public MessageProcessor getMessageProcessor() {
		return messageProcessor;
	}

	public void setMessageProcessor(MessageProcessor messageProcessor) {
		this.messageProcessor = messageProcessor;
	} 
	
	
	public ConsumerHandler getConsumerHandler() {
		return consumerHandler;
	}

	public void setConsumerHandler(ConsumerHandler consumerHandler) {
		this.consumerHandler = consumerHandler;
	} 
	
	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}  
	
	
	public int getConsumerHandlerPoolSize() {
		return consumerHandlerPoolSize;
	}

	public void setConsumerHandlerPoolSize(int consumerHandlerPoolSize) {
		this.consumerHandlerPoolSize = consumerHandlerPoolSize;
	} 

	public boolean isConsumerHandlerRunInPool() {
		return consumerHandlerRunInPool;
	}

	public void setConsumerHandlerRunInPool(boolean consumerHandlerRunInPool) {
		this.consumerHandlerRunInPool = consumerHandlerRunInPool;
	}
	
	

	public int getInFlightMessageCount() {
		return inFlightMessageCount;
	}

	public void setInFlightMessageCount(int inFlightMessageCount) {
		this.inFlightMessageCount = inFlightMessageCount;
	}

	@Override
	public ConsumerServiceConfig clone() {
		return (ConsumerServiceConfig) super.clone();
	}

}
