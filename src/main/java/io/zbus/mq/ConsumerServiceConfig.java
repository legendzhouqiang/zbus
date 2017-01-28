package io.zbus.mq;

public class ConsumerServiceConfig extends MqConfig{ 
	private int consumerCount = 4;  
	private int threadPoolSize = 64; 
	private int maxInFlightMessage = 100;
	private boolean verbose = false; 
	private boolean parallelMode = true;
	
	private ConsumerHandler consumerHandler; 
	private MessageProcessor messageProcessor;  
	

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
	
	public boolean isParallelMode() {
		return parallelMode;
	}

	public void setParallelMode(boolean parallelMode) {
		this.parallelMode = parallelMode;
	}

	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}   

	public int getMaxInFlightMessage() {
		return maxInFlightMessage;
	}

	public void setMaxInFlightMessage(int maxInFlightMessage) {
		this.maxInFlightMessage = maxInFlightMessage;
	}

	@Override
	public ConsumerServiceConfig clone() {
		return (ConsumerServiceConfig) super.clone();
	}

}
