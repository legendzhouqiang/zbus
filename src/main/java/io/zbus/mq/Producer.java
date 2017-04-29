package io.zbus.mq;

import java.io.IOException;
import java.util.List;

import io.zbus.mq.Broker.ServerSelector;
import io.zbus.mq.Protocol.ServerAddress;
import io.zbus.mq.Protocol.TopicInfo;
 

public class Producer extends MqAdmin{  
	private ServerSelector produceServerSelector;  
	
	public Producer(ProducerConfig config){
		super(config); 
		
		this.produceServerSelector = config.getProduceServerSelector();
		if(this.produceServerSelector == null){
			this.produceServerSelector = new DefaultProduceServerSelector();
		} 
	} 
	
	public Message publish(Message msg, int timeout) throws IOException, InterruptedException {
		MqClientPool[] poolArray = broker.selectClient(this.produceServerSelector, msg.getTopic());
		if(poolArray.length < 1){
			throw new MqException("Missing MqClient for publishing message: " + msg);
		}
		MqClientPool pool = poolArray[0]; 
		MqClient client = null;
		try {
			client = pool.borrowClient();  
			return configClient(client).produce(msg, timeout);
		} finally {
			pool.returnClient(client);
		} 
	}   
	
	public Message publish(Message msg) throws IOException, InterruptedException {
		return publish(msg, invokeTimeout);
	}
	
	public void publishAsync(Message msg, MessageCallback callback) throws IOException {
		MqClientPool[] poolArray = broker.selectClient(this.produceServerSelector, msg.getTopic());
		if(poolArray.length < 1){
			throw new MqException("Missing MqClient for publishing message: " + msg);
		}
		MqClientPool pool = poolArray[0]; 
		MqClient client = null;
		try {
			client = pool.borrowClient();
			configClient(client).produceAsync(msg, callback);
		} finally {
			pool.returnClient(client);
		} 
	}   
	
	public ServerSelector getProduceServerSelector() {
		return produceServerSelector;
	}

	public void setProduceServerSelector(ServerSelector produceServerSelector) {
		this.produceServerSelector = produceServerSelector;
	}



	public class DefaultProduceServerSelector implements ServerSelector{ 
		@Override
		public ServerAddress[] select(BrokerRouteTable table, String topic) { 
			int serverCount = table.serverTable().size();
			if (serverCount == 0) {
				return null;
			}
			List<TopicInfo> topicList = table.topicTable().get(topic);
			if (topicList == null || topicList.size() == 0) {
				return new ServerAddress[]{table.randomServerInfo().serverAddress};
			}
			TopicInfo target = topicList.get(0);
			for (int i = 1; i < topicList.size(); i++) {
				TopicInfo current = topicList.get(i);
				if (target.consumerCount < current.consumerCount) { //consumer count decides
					target = current;
				}
			}
			return new ServerAddress[]{target.serverAddress};
		} 
	} 
}
