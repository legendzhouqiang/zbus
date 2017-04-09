package io.zbus.mq.broker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MessageCallback;
import io.zbus.mq.MessageInvoker;
import io.zbus.mq.MqException;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TrackerInfo;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.net.Client.MsgHandler;
import io.zbus.net.EventDriver;
import io.zbus.net.Session;
import io.zbus.util.JsonUtil;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;

public class TrackBroker implements Broker { 
	private static final Logger log = LoggerFactory.getLogger(TrackBroker.class);
	private Map<String, SingleBroker> brokerMap = new ConcurrentHashMap<String, SingleBroker>();  
	private ServerSelector serverSelector = new DefaultServerSelector(); 
	private RouteTable routeTable = new RouteTable();
	private List<ServerNotifyListener> listeners = new ArrayList<ServerNotifyListener>(); 
	private EventDriver eventDriver; 
	private final BrokerConfig config;  
	
	private Map<String, MessageClient> trackSubscribers = new ConcurrentHashMap<String, MessageClient>(); 
	
	private CountDownLatch ready = new CountDownLatch(1);
	private boolean waitCheck = true;
	
	public TrackBroker(BrokerConfig config) throws IOException{ 
		this.config = config.clone();
		this.eventDriver = new EventDriver();
		
		String[] serverAddressList = config.getBrokerAddress().split("[;, ]");
		if(serverAddressList.length <= 0){
			throw new IllegalArgumentException("brokerAddress illegal: " + config.getBrokerAddress());
		}
		for(String serverAddress : serverAddressList){
			serverAddress = serverAddress.trim();
			if(serverAddress.isEmpty()) continue;
			subscribeToTracker(serverAddress);
		} 
	}
	
	public TrackBroker(String brokerAddress) throws IOException{ 
		this(new BrokerConfig(brokerAddress));
	} 
	
	private void subscribeToTracker(final String serverAddress){
		final MessageClient client = new MessageClient(serverAddress, eventDriver);  
		client.onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException { 
				log.warn("Disconnected from tracker(%s)", serverAddress); 
				if(routeTable.serverInfo(serverAddress) != null){
					routeTable.removeServer(serverAddress);
				}
				client.ensureConnectedAsync(); 
			}  
		});
		
		client.onConnected(new ConnectedHandler() {
			@Override
			public void onConnected() throws IOException { 
				log.info("Connected to tracker(%s)", serverAddress);  
				
				Message req = new Message();  
				req.setCommand(Protocol.TRACK_SUB);
				req.setAck(false); 
				client.invokeAsync(req);
			}
		}); 
		
		client.onMessage(new MsgHandler<Message>() {  
			@Override
			public void handle(Message msg, Session session) throws IOException { 
				if(Protocol.TRACK_PUB.equals(msg.getCommand())){ 
					TrackerInfo trackerInfo = JsonUtil.parseObject(msg.getBodyString(), TrackerInfo.class);
					onTrackInfoUpdate(trackerInfo);
				}
			}
		});
		
		client.ensureConnectedAsync();
		trackSubscribers.put(serverAddress, client); 
	}
	
	private void onTrackInfoUpdate(TrackerInfo trackerInfo) throws IOException {
		List<String> toRemove = routeTable.updateVotes(trackerInfo);
		if(!toRemove.isEmpty()){ 
			for(String serverAddress : toRemove){
				removeServer(serverAddress);
			}
		}
		
		for(String serverAddress : trackerInfo.liveServerList){
			SingleBroker broker = brokerMap.get(serverAddress);
			if(broker == null){
				addServer(serverAddress);
				broker = brokerMap.get(serverAddress);
			} 
			try{
				updateServerInfo(broker); 
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	} 
	
	private void updateServerInfo(SingleBroker broker) throws IOException{
		Message message = new Message();
		message.setCommand(Protocol.INFO);  
		broker.invokeAsync(message, new MessageCallback() { 
			@Override
			public void onReturn(Message result) {
				ServerInfo serverInfo = JsonUtil.parseObject(result.getBodyString(), ServerInfo.class);
				routeTable.update(serverInfo);
				ready.countDown();
			}
		}); 
	}
	
	@Override
	public void addServer(final String serverAddress) throws IOException {  	
		final SingleBroker broker;
		synchronized (brokerMap) {
			if(brokerMap.containsKey(serverAddress)){
				return;
			}  
			broker = createBroker(serverAddress);
			brokerMap.put(serverAddress, broker);  
		}    
		
		broker.onConnected(new ConnectedHandler() { 
			@Override
			public void onConnected() {
				for(final ServerNotifyListener listener : listeners){
					eventDriver.getGroup().submit(new Runnable() { 
						@Override
						public void run() {  
							listener.onServerJoin(broker);
						}
					});
				}
			}
		});
		
		broker.onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() { 
				try {
					removeServer(serverAddress);
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		});
		
		broker.dectectConnection();  
	} 
	
	@Override
	public void removeServer(final String serverAddress) throws IOException { 
		final Broker broker;
		synchronized (brokerMap) { 
			broker = brokerMap.remove(serverAddress);
			if(broker == null) return;
			
			eventDriver.getGroup().schedule(new Runnable() {
				
				@Override
				public void run() {
					try {
						broker.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					} 
				}
			}, 1000, TimeUnit.MILLISECONDS); //delay 1s to close to wait other service depended on this broker
			
		}    
		
		for(final ServerNotifyListener listener : listeners){
			eventDriver.getGroup().submit(new Runnable() { 
				@Override
				public void run() { 
					listener.onServerLeave(serverAddress);
				}
			});
		}
	}

	
	private SingleBroker createBroker(String serverAddress) throws IOException{
		BrokerConfig config = this.config.clone();
		config.setBrokerAddress(serverAddress);
		SingleBroker broker = new SingleBroker(config);
		return broker;
	} 


	
	@Override
	public MessageInvoker selectForProducer(String topic) throws IOException {
		checkReady();
		String serverAddress = serverSelector.selectForProducer(routeTable, topic); 
		if(serverAddress == null){
			throw new MqException("Missing broker for topic=" + topic);
		}
		Broker broker = brokerMap.get(serverAddress);
		if(broker == null){
			throw new IllegalStateException("Can not find server=" + serverAddress);
		}
		
		return broker.selectForProducer(topic);
	}

	@Override
	public MessageInvoker selectForConsumer(String topic) throws IOException {
		checkReady();
		String serverAddress = serverSelector.selectForConsumer(routeTable, topic);
		if(serverAddress == null){
			throw new MqException("Missing broker for topic=" + topic);
		}
		Broker broker = brokerMap.get(serverAddress);
		if(broker == null){
			throw new IllegalStateException("Can not find server=" + serverAddress);
		}
		return broker.selectForConsumer(topic);
	}
	
	@Override
	public void releaseInvoker(MessageInvoker invoker) throws IOException { 
		if(!(invoker instanceof MessageClient)){
			return; 
		}
		
		MessageClient client = (MessageClient)invoker;
		String serverAddress = client.attr(Protocol.SERVER);
		if(serverAddress == null){
			throw new IllegalStateException("Can not find server in MessageClient attributes");
		}
		Broker broker = brokerMap.get(serverAddress);
		if(broker == null){
			throw new IllegalStateException("Can not find server=" + serverAddress);
		}
	    broker.releaseInvoker(invoker);
	}

	@Override
	public List<Broker> availableServerList() {  
		return new ArrayList<Broker>(brokerMap.values());
	}

	@Override
	public void configServerSelector(ServerSelector selector) {
		this.serverSelector = selector;
	} 
	
	@Override
	public void addServerNotifyListener(ServerNotifyListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeServerNotifyListener(ServerNotifyListener listener) {
		this.listeners.remove(listener);
	} 
	
	@Override
	public void close() throws IOException {
		for(MessageClient client : trackSubscribers.values()){
			client.close();
		}
		trackSubscribers.clear();
		synchronized (brokerMap) {
			for(Broker broker : brokerMap.values()){ 
				broker.close();
			}
			brokerMap.clear();
		}  
		
		eventDriver.close();
	} 
	
	private void checkReady(){
		if(waitCheck){
			try {
				ready.await(3000, TimeUnit.MILLISECONDS); //TODO make it configurable
			} catch (InterruptedException e) {
				//ignore 
			}
			waitCheck = false;
		}
	}
	
	
	@Override
	public String brokerAddress() { 
		return config.getBrokerAddress();
	}
}
