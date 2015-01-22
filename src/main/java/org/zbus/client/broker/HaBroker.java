package org.zbus.client.broker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.client.Broker;
import org.zbus.client.ClientHint;
import org.zbus.client.ZbusException;
import org.zbus.common.MqInfo;
import org.zbus.common.TrackTable;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.RemotingClient;

public class HaBroker implements Broker, TrackListener {
	private static final Logger log = LoggerFactory.getLogger(HaBroker.class);
	private volatile TrackTable trackTable = new TrackTable();
	private String trackAddressList;
	private HaBrokerConfig config;
	public TrackAgent trackAgent;
 
	private Map<String, SingleBroker> brokers = new ConcurrentHashMap<String, SingleBroker>();

	public HaBroker(HaBrokerConfig config) {
		this.config = config;
		this.trackAddressList = config.getTrackAddrList(); 
		try {
			this.trackAgent = new TrackAgent(this.trackAddressList);
			this.trackAgent.addTrackListener(this);
		} catch (IOException e) { 
			log.error(e.getMessage(), e);
		}
	}
	

	@Override
	public RemotingClient getClient(ClientHint hint) {
		SingleBroker broker = null;
		//1) broker address first
		if(hint.getBroker() != null){
			broker = getBrokerByAddress(hint.getBroker());
		}
		if(broker != null) {
			return broker.getClient(hint);
		}
		//2) mq identity comes second
		if(hint.getMq() != null){
			broker = getBrokerByMq(hint.getMq());
		}
		if(broker != null) {
			return broker.getClient(hint);
		}
		//3) cluster of request_ip comes thirdly
		if(hint.getRequestIp() != null){
			broker = getBrokerByRequestIp(hint.getRequestIp());
		}
		
		if(broker != null) {
			return broker.getClient(hint);
		}
		
		//4) default to random one
		List<SingleBroker> list = new ArrayList<SingleBroker>(this.brokers.values());
		int count = list.size();
		if(count == 0){
			throw new ZbusException("unavailabe broker for client");
		}
		broker = list.get(new Random().nextInt(count));
		return broker.getClient(hint); 
	}
	
	

	@Override
	public void closeClient(RemotingClient client) {
		String brokerAddress = client.attr("broker");
		if(brokerAddress == null){//ignore
			log.warn("client missing broker attribute, can not close in HA mode");
			return;
		}
		SingleBroker broker = getBrokerByAddress(brokerAddress);
		if(broker == null){
			log.warn("client's broker owner attribute, can not close in HA mode");
			return;
		}
		broker.closeClient(client);
	}
	
	
	private SingleBroker getBrokerByAddress(String address){
		return this.brokers.get(address);
	}
	
	private SingleBroker getBrokerByMq(String mq){
		List<MqInfo> mqInfos = trackTable.getMqInfo(mq);
		if(mqInfos == null || mqInfos.size() == 0) return null;
		//random choose one
		int idx = new Random().nextInt(mqInfos.size());
		
		MqInfo mqInfo = mqInfos.get(idx); 
		String address = mqInfo.getBroker();
		
		return getBrokerByAddress(address);
	}
	
	private SingleBroker getBrokerByRequestIp(String ip){
		List<SingleBroker> list = new ArrayList<SingleBroker>(this.brokers.values());
		int count = list.size();
		if(count == 0) return null;
		
		//clustered by requestIp
		int idx = ip.hashCode()%count;
		return list.get(idx);
	}
	
	
	@Override
	public void onTrackTableUpdated(TrackTable trackTable) {
		this.trackTable = trackTable; 
		for (String brokerAddress : trackTable.getBrokerList()) {
			SingleBroker broker = this.brokers.get(brokerAddress);
			if (broker != null) continue;
			
			SingleBrokerConfig singleConfig = new SingleBrokerConfig(
					this.config.getPoolConfig());
			singleConfig.setBrokerAddress(brokerAddress);
			
			broker = new SingleBroker(singleConfig);
			this.brokers.put(brokerAddress, broker);
		}
		
		Iterator<Entry<String, SingleBroker>> iter = this.brokers.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, SingleBroker> entry = iter.next();
			String brokerAddress = entry.getKey();
			SingleBroker broker = entry.getValue();
			if(!this.trackTable.getBrokerList().contains(brokerAddress)){
				broker.destroy();
				iter.remove();
			}
		} 
	}
	
	@Override
	public void destroy() { 
		for (SingleBroker broker : this.brokers.values()) {
			broker.destroy();
		}
	}
}
