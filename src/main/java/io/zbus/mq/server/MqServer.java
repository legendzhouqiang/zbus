package io.zbus.mq.server;

import static io.zbus.kit.ConfigKit.isBlank;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.zbus.kit.ConfigKit;
import io.zbus.kit.NetKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.net.MessageServer;
import io.zbus.net.EventDriver;
import io.zbus.net.Session;

public class MqServer implements Closeable{ 
	private static final Logger log = LoggerFactory.getLogger(MqServer.class); 
	
	private final Map<String, Session> sessionTable = new ConcurrentHashMap<String, Session>();
	private final Map<String, MessageQueue> mqTable = new ConcurrentHashMap<String, MessageQueue>();
	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
	
	private MqServerConfig config;   
	private String serverAddress = "";    
	private EventDriver eventDriver; 
	
	private MessageServer messageServer;
	private MqAdaptor mqAdaptor;  
	private Tracker tracker;
	private MessageTracer messageTracer;
	
	public MqServer(){
		this(new MqServerConfig());
	}
	
	public MqServer(MqServerConfig config){  
		this.config = config;   
		this.eventDriver = new EventDriver();  
		
		if(eventDriver.getSslContext() == null){
			if (!isBlank(config.sslCertificateFile) && !isBlank(config.sslPrivateKeyFile)){ 
				InputStream certStream = getClass().getClassLoader().getResourceAsStream(config.sslCertificateFile);
				InputStream privateKeyStream = getClass().getClassLoader().getResourceAsStream(config.sslPrivateKeyFile);
				if(certStream == null){
					log.warn("Certificate File: " + config.sslCertificateFile + " not exists");
				}
				if(privateKeyStream == null){
					log.warn("PrivateKey File: " + config.sslPrivateKeyFile + " not exists");
				}
				if(certStream != null && privateKeyStream != null){
					eventDriver.setServerSslContext(certStream, privateKeyStream);
					try {
						certStream.close();
					} catch (IOException e) {
						//ignore
					}
					try {
						privateKeyStream.close();
					} catch (IOException e) {
						//ignore
					} 
				}
			}
		}
		
		String host = config.serverHost;
		if("0.0.0.0".equals(host)){
			host = NetKit.getLocalIp(config.serverMainIpOrder);
		}
		serverAddress = host+":"+config.serverPort; 
		if(!StrKit.isEmpty(config.serverName)){
			serverAddress = config.serverName + ":"+config.serverPort; 
		}
		
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
		tracker = new Tracker(this, true); //TODO configure whether this server in track or not
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
		String trackerList = config.getTrackServerList();
		if(!StrKit.isEmpty(trackerList)){ //tracker try to join upstream Tracker.
			tracker.joinUpstream(trackerList);
		}
		
		messageServer = new MessageServer(eventDriver);   
		messageServer.start(config.serverHost, config.serverPort, mqAdaptor);  
		 
		long end = System.currentTimeMillis();
		log.info("Zbus started sucessfully in %d ms", (end-start)); 
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

	public String getServerAddress() {
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
		info.serverAddress = serverAddress;
		info.topicMap = table;
		info.trackedServerList = tracker.trackerInfo().trackedServerList;

		String serverList = config.getTrackServerList();
		if (serverList == null) {
			serverList = "";
		}
		serverList = serverList.trim();

		info.trackerList = new ArrayList<String>();
		for (String s : serverList.split("[;, ]")) {
			s = s.trim();
			if ("".equals(s))
				continue;
			info.trackerList.add(s);
		}
		return info;
	}

	public static void main(String[] args) throws Exception {
		MqServerConfig config = new MqServerConfig(); 
		String xmlConfigFile = ConfigKit.option(args, "-conf", "conf/zbus.xml");
		try{
			config.loadFromXml(xmlConfigFile); 
		} catch(Exception ex){ 
			String message = xmlConfigFile + " config error encountered\n" + ex.getMessage();
			System.err.println(message);
			log.warn(message); 
			return;
		} 
		
		final MqServer server = new MqServer(config);
		server.start(); 
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


