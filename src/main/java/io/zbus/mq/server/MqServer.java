package io.zbus.mq.server;
 

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.zbus.kit.ConfigKit;
import io.zbus.kit.NetKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Protocol.ServerAddress;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.net.MessageServer;
import io.zbus.net.EventDriver;
import io.zbus.net.Session;

public class MqServer implements Closeable{ 
	private static final Logger log = LoggerFactory.getLogger(MqServer.class); 
	
	private final Map<String, Session> sessionTable = new ConcurrentHashMap<String, Session>();
	private final Map<String, MessageQueue> mqTable = new ConcurrentSkipListMap<String, MessageQueue>(String.CASE_INSENSITIVE_ORDER);
	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
	
	private MqServerConfig config;   
	private ServerAddress serverAddress;    
	private EventDriver eventDriver; 
	
	private MessageServer messageServer;
	private MqAdaptor mqAdaptor;  
	private Tracker tracker;
	private MessageTracer messageTracer;
	
	private AtomicLong infoVersion = new AtomicLong(System.currentTimeMillis());
	
	public MqServer(){
		this(new MqServerConfig());
	}
	
	public MqServer(String configFile){
		this(new MqServerConfig(configFile));
	}
	
	public MqServer(MqServerConfig config){  
		this.config = config.clone();   
		this.eventDriver = new EventDriver();  
		
		if (config.sslEnabled){
			if(!StrKit.isEmpty(config.sslCertFile) && !StrKit.isEmpty(config.sslKeyFile)){  
				try{
					eventDriver.setServerSslContext(config.sslCertFile, config.sslKeyFile);
				} catch (Exception e) {
					e.printStackTrace();
					log.error("SSL disabled: " + e.getMessage());
				}
			} else {
				log.warn("SSL disabled, since SSL certificate file and private file not configured properly");
			}
		}
		
		String host = config.serverHost;
		if("0.0.0.0".equals(host)){
			host = NetKit.getLocalIp();
		}
		String address = host+":"+config.serverPort;
		if(!StrKit.isEmpty(config.serverName)){
			address = config.serverName + ":"+config.serverPort; 
		} 
		serverAddress = new ServerAddress(address, eventDriver.isSslEnabled()); 
		
		this.scheduledExecutor.scheduleAtFixedRate(new Runnable() { 
			public void run() {  
				Iterator<Entry<String, MessageQueue>> iter = mqTable.entrySet().iterator();
		    	while(iter.hasNext()){
		    		Entry<String, MessageQueue> e = iter.next();
		    		MessageQueue mq = e.getValue(); 
		    		mq.cleanInactiveSessions();
		    	}
			}
		}, 1000, config.cleanMqInterval, TimeUnit.MILLISECONDS); 
		  
		messageTracer = new MessageTracer();
		tracker = new Tracker(this, config.sslCertFileTable, 
				!config.trackerModeOnly, config.trackReportInterval);
		
		mqAdaptor = new MqAdaptor(this);   
	} 
	
	public void start() throws Exception{  
		log.info("Zbus starting...");
		long start = System.currentTimeMillis(); 
		
		messageTracer.start();
		
		mqAdaptor.setVerbose(config.verbose);
		try {
			mqAdaptor.loadDiskQueue();
		} catch (IOException e) {
			log.error("Load Message Queue Error: " + e);
		}   
		 
		tracker.joinUpstream(config.getTrackerList());
		
		messageServer = new MessageServer(eventDriver);   
		messageServer.start(config.serverHost, config.serverPort, mqAdaptor);  
		 
		long end = System.currentTimeMillis();
		log.info("Zbus(%s) started sucessfully in %d ms", serverAddress, (end-start)); 
	}
	 
	@Override
	public void close() throws IOException {   
		scheduledExecutor.shutdown();   
		mqAdaptor.close();
		if(messageServer != null){
			messageServer.close();
		} 
		if(eventDriver != null){
			eventDriver.close(); 
		}
	}  
    
	public Map<String, MessageQueue> getMqTable() {
		return mqTable;
	} 

	public Map<String, Session> getSessionTable() {
		return sessionTable;
	}  

	public ServerAddress getServerAddress() {
		return serverAddress;
	}  

	public MqServerConfig getConfig() {
		return config;
	}   
	
	public MqAdaptor getMqAdaptor() {
		return mqAdaptor;
	}
	
	public EventDriver getEventDriver() {
		return eventDriver;
	}  

	public Tracker getTracker() {
		return tracker;
	}
	
	public MessageTracer getMessageTracer() {
		return messageTracer;
	}
	
	public ServerInfo serverInfo() {
		Map<String, TopicInfo> table = new HashMap<String, TopicInfo>();
		for (Map.Entry<String, MessageQueue> e : this.mqTable.entrySet()) {
			TopicInfo info = e.getValue().topicInfo();
			info.serverAddress = serverAddress;
			table.put(e.getKey(), info);
		}
		ServerInfo info = new ServerInfo(); 
		info.infoVersion = infoVersion.getAndIncrement();
		info.serverAddress = serverAddress;
		info.trackerList = this.tracker.liveTrackerList();
		info.topicTable = table; 
 
		return info;
	}

	public static void main(String[] args) throws Exception { 
		String configFile = ConfigKit.option(args, "-conf", "conf/zbus.xml"); 
		
		final MqServer server;
		try{
			server = new MqServer(configFile);
			server.start(); 
		} catch (Exception e) { 
			e.printStackTrace(System.err);
			log.warn(e.getMessage(), e); 
			return;
		} 
		
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try { 
					server.close();
					log.info("MqServer shutdown completed");
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});    
	} 
}


