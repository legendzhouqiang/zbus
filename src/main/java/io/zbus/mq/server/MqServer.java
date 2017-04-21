package io.zbus.mq.server;

import static io.zbus.kit.ConfigKit.isBlank;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.zbus.kit.ConfigKit;
import io.zbus.kit.NetKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
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
	
	private MessageServer httpServer;
	private MqAdaptor mqAdaptor;  
	private Tracker tracker;
	private TraceService traceService;
	
	public MqServer(){
		this(new MqServerConfig());
	}
	
	public MqServer(MqServerConfig config){  
		this.config = config;   
		this.eventDriver = new EventDriver();  
		
		if(eventDriver.getSslContext() == null){
			if (!isBlank(config.sslCertificateFile) && !isBlank(config.sslPrivateKeyFile)){ 
				File sslCert = new File(config.sslCertificateFile);
				File sslPriv = new File(config.sslPrivateKeyFile);
				if(!sslCert.exists()){
					log.warn("Certificate File: " + config.sslCertificateFile + " not exists");
				}
				if(!sslPriv.exists()){
					log.warn("PrivateKey File: " + config.sslCertificateFile + " not exists");
				}
				if(sslCert.exists() && sslPriv.exists()){
					eventDriver.setSslContext(sslCert, sslPriv);
				}
			}
		}
		
		String host = config.serverHost;
		if("0.0.0.0".equals(host)){
			host = NetKit.getLocalIp(config.serverMainIpOrder);
		}
		serverAddress = host+":"+config.serverPort; 
		
		this.scheduledExecutor.scheduleAtFixedRate(new Runnable() { 
			public void run() {  
				Iterator<Entry<String, MessageQueue>> iter = mqTable.entrySet().iterator();
		    	while(iter.hasNext()){
		    		Entry<String, MessageQueue> e = iter.next();
		    		MessageQueue mq = e.getValue(); 
		    		mq.cleanAllSessions();
		    	}
			}
		}, 1000, config.cleanMqInterval, TimeUnit.MILLISECONDS); 
		  
		traceService = new TraceService();
		tracker = new Tracker(this, config.getTrackServerList(), true);//TODO configure 
		//trackService should be ahead of mqAdaptor to initialize
		mqAdaptor = new MqAdaptor(this);   
	} 
	
	public void start() throws Exception{  
		log.info("zbus starting...");
		long start = System.currentTimeMillis(); 
		
		traceService.start();
		
		mqAdaptor.setVerbose(config.verbose);
		try {
			mqAdaptor.loadMQ();
		} catch (IOException e) {
			log.error("LoadMQ error: " + e);
		}   
		tracker.connect();
		
		httpServer = new MessageServer(eventDriver);   
		httpServer.start(config.serverHost, config.serverPort, mqAdaptor);  
		 
		long end = System.currentTimeMillis();
		log.info("zbus started sucessfully in %d ms", (end-start)); 
	}
	 
	@Override
	public void close() throws IOException {   
		scheduledExecutor.shutdown();   
		mqAdaptor.close();
		if(httpServer != null){
			httpServer.close();
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
	
	public TraceService getTraceService() {
		return traceService;
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


