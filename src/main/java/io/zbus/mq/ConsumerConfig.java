package io.zbus.mq;
 
import io.zbus.mq.Broker.ServerSelector;

public class ConsumerConfig extends MqConfig {  
	protected Topic topic; 
	protected ConsumeGroup consumeGroup; 
	protected Integer consumeWindow; 
	protected int consumeTimeout = 120000;// 2 minutes  
	
	protected MessageHandler messageHandler;   
	protected int connectionCount = 1;  
	
	protected ServerSelector consumeServerSelector; 
	
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
	
	public void setConsumeGroup(String group) {
		setConsumeGroup(group, null);
	} 
	
	public void setConsumeGroup(String group, String msgFilter) {
		ConsumeGroup consumerGroup = new ConsumeGroup();
		consumerGroup.setGroupName(group);
		consumerGroup.setFilter(msgFilter);
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

	public Topic getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = new Topic(topic);
	} 
	
	public void setTopic(Topic topic) {
		this.topic = topic;
	}
	
	public void setTopic(String topic, Integer topicMask) {
		this.topic = new Topic(topic);
		this.topic.setMask(topicMask);
	}
	
	public void setTopicMask(Integer topicMask) {
		if(this.topic == null) {
			this.topic = new Topic(); 
		}
		this.topic.setMask(topicMask);
	}

	public ServerSelector getConsumeServerSelector() {
		return consumeServerSelector;
	}

	public void setConsumeServerSelector(ServerSelector consumeServerSelector) {
		this.consumeServerSelector = consumeServerSelector;
	} 

	public MessageHandler getMessageHandler() {
		return messageHandler;
	}

	public void setMessageHandler(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}  

	public int getConnectionCount() {
		return connectionCount;
	}

	public void setConnectionCount(int connectionCount) {
		this.connectionCount = connectionCount;
	} 

	@Override
	public ConsumerConfig clone() { 
		return (ConsumerConfig)super.clone();
	}
	
}
