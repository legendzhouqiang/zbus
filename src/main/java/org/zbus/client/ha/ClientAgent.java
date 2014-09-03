package org.zbus.client.ha;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.client.ClientBuilder;
import org.zbus.client.ClientPool;
import org.zbus.common.MqInfo;
import org.zbus.common.TrackTable;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.ClientDispatcherManager;
import org.zbus.remoting.RemotingClient;
 
 
public class ClientAgent implements ClientPool, ClientBuilder, TrackListener{ 
	private static final Logger log = LoggerFactory.getLogger(ClientAgent.class);
	
	private String seedBroker = "127.0.0.1:15555";
	
	private ConcurrentHashMap<String, ClientPool> poolTable = new ConcurrentHashMap<String, ClientPool>(); 
	
	private volatile TrackTable trackTable = new TrackTable(); 

	private Tracker tracker;
	private AgentConfig config; 
	private ClientDispatcherManager clientMgr;   
		
	public ClientAgent(AgentConfig config) throws IOException{
		this(config, null);
	} 
	
	public ClientAgent(AgentConfig config, int timeout) throws IOException{
		this(config, null, timeout);
	} 
	
	public ClientAgent(AgentConfig config, ClientDispatcherManager clientMgr) throws IOException{
		this(config, clientMgr, 3000);
	}
	
	public ClientAgent(AgentConfig config, ClientDispatcherManager clientMgr, int timeout) throws IOException {  
		this.config = config;
		this.seedBroker = this.config.getSeedBroker();
		this.clientMgr = clientMgr;  
		
		this.tracker = new Tracker(this.config.getTrackServerList(), clientMgr);
		this.tracker.addTrackListener(this);
		
		this.tracker.waitForReady(timeout);
		
		ClientPool pool = poolTable.get(this.seedBroker);
		if(pool == null){ 
			pool = PoolFactory.createPool(this.config, this.seedBroker, this.clientMgr);
			if(this.poolTable.putIfAbsent(this.seedBroker, pool) != null){
				pool.destroy();
			}
		}
	} 
	
	
	public RemotingClient createClientForMQ(String mq){
		List<MqInfo> mqInfoList = this.trackTable.getMqInfo(mq);
		String broker = this.seedBroker;
		if(mqInfoList != null && mqInfoList.size() > 0){
			broker = mqInfoList.get(0).getBroker(); 
		}  
		if(this.trackTable.getBrokerList().size() > 0){
			broker = this.trackTable.getBrokerList().get(0);
		}
		
		return new RemotingClient(broker, this.clientMgr);
	} 
	
	public RemotingClient createClientForBroker(String broker){
		if(!this.trackTable.getBrokerList().contains(broker)){
			throw new IllegalArgumentException(String.format("Broker(%s) not found in track table", broker));
		}
		return new RemotingClient(broker, this.clientMgr);
	}
	
	public RemotingClient borrowClient(String mq) throws Exception { 
		MqInfo mqInfo = trackTable.nextMqInfo(mq);
		String broker = this.seedBroker; //default to seed
		if(mqInfo != null){
			broker = mqInfo.getBroker(); 
		} 
		
		ClientPool pool = poolTable.get(broker);
		if(pool == null){
			throw new IllegalStateException(String.format("Pool(%s) not found", broker));
		}
		
		RemotingClient client = pool.borrowClient(mq);  
		client.attr("broker", broker);
		return client;
	}
	
	public List<RemotingClient> borrowEachClient(String mq) throws Exception { 
		List<RemotingClient> clients = new ArrayList<RemotingClient>(); 
		List<String> brokerList = new ArrayList<String>();
		
		List<MqInfo> mqInfoList = trackTable.getMqInfo(mq);
		if(mqInfoList == null || mqInfoList.size()==0){
			brokerList.add(this.seedBroker); 
		} else {
			for(MqInfo mqInfo : mqInfoList){  
				brokerList.add(mqInfo.getBroker());
			}
		}
		
		for(String broker : brokerList){  
			ClientPool pool = poolTable.get(broker);
			if(pool == null){
				continue;
			}
			RemotingClient client = pool.borrowClient(null);  
			client.attr("broker", broker);
			clients.add(client); 
		}
		return clients;
	}
	
	public void invalidateClient(RemotingClient client){
		ClientPool pool = findPool(client);
		try {
			pool.invalidateClient(client);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public void returnClient(RemotingClient client) throws Exception { 
		ClientPool pool = findPool(client);
		pool.returnClient(client); 
	} 
	 
	public void returnClient(List<RemotingClient> clients) throws Exception { 
		for(RemotingClient client : clients){
			try{ returnClient(client); } catch (Exception e) {}
		}
	}
	
 
	public void destroy() { 
		Iterator<Entry<String, ClientPool>> iter = poolTable.entrySet().iterator();
		while(iter.hasNext()){ 
			Map.Entry<String, ClientPool> e = iter.next();
			e.getValue().destroy(); 
			iter.remove();
		}  
	}    
	
	private ClientPool findPool(RemotingClient client){
		String broker = client.attr("broker");
		if(broker == null){
			throw new IllegalStateException("Client missing broker attribute");
		}
		ClientPool pool = poolTable.get(broker);
		if(pool == null){
			throw new IllegalStateException(String.format("Broker(%s) pool not found", broker));
		}
		return pool;
	}

	
	@Override
	public void onTrackTableUpdated(TrackTable trackTable) { 
		this.trackTable = trackTable;  
		for(String broker : trackTable.getBrokerList()){
			ClientPool pool = poolTable.get(broker);
			if(pool != null){ 
				continue;
			}
			pool = PoolFactory.createPool(this.config, broker, this.clientMgr);
			poolTable.put(broker, pool); 
		}
	}
}
