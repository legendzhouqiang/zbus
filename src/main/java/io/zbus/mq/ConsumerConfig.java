package io.zbus.mq;
 
import io.zbus.mq.Broker.ServerSelector;

public class ConsumerConfig extends MqConfig {  
	protected String topic;
	protected ConsumeGroup consumeGroup; 
	protected Integer consumeWindow; 
	protected int consumeTimeout = 120000;// 2 minutes 
	 
	
	protected ServerSelector consumeServerSelector;
	
	protected ConsumeHandler consumeHandler; 
	protected MessageProcessor messageProcessor;  
	protected int consumeThreadCount = 4;
	protected int consumeRunnerPoolSize = 64; 
	protected int maxInFlightMessage = 100;
	
	public ConsumerConfig(){
		
	}
	
	public ConsumerConfig(Broker broker){
		super(broker);
	}
	
	public ConsumeGroup getConsumeGroup() {
		return consumeGroup;
	} 
	
	public void setConsumeGroup(ConsumeGroup consumerGroup) {
		this.consumeGroup = consumerGroup;
	} 

	public Integer getConsumeWindow() {
		return consumeWindow;
	} 

	public void setConsumeWindow(Integer consumeWindow) {
		this.consumeWindow = consumeWindow;
	} 

	public int getConsumeTimeout() {
		return consumeTimeout;
	} 

	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	} 

	public ServerSelector getConsumeServerSelector() {
		return consumeServerSelector;
	}

	public void setConsumeServerSelector(ServerSelector consumeServerSelector) {
		this.consumeServerSelector = consumeServerSelector;
	} 

	public ConsumeHandler getConsumeHandler() {
		return consumeHandler;
	}

	public void setConsumeHandler(ConsumeHandler consumeHandler) {
		this.consumeHandler = consumeHandler;
	}

	public MessageProcessor getMessageProcessor() {
		return messageProcessor;
	}

	public void setMessageProcessor(MessageProcessor messageProcessor) {
		this.messageProcessor = messageProcessor;
	} 
	
	public int getConsumeRunnerPoolSize() {
		return consumeRunnerPoolSize;
	}

	public void setConsumeRunnerPoolSize(int consumeRunnerPoolSize) {
		this.consumeRunnerPoolSize = consumeRunnerPoolSize;
	}

	public int getMaxInFlightMessage() {
		return maxInFlightMessage;
	}

	public void setMaxInFlightMessage(int maxInFlightMessage) {
		this.maxInFlightMessage = maxInFlightMessage;
	} 

	public int getConsumeThreadCount() {
		return consumeThreadCount;
	}

	public void setConsumeThreadCount(int consumeThreadCount) {
		this.consumeThreadCount = consumeThreadCount;
	}

	@Override
	public ConsumerConfig clone() { 
		return (ConsumerConfig)super.clone();
	}
	
}
