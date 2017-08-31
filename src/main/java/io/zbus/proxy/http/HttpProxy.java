package io.zbus.proxy.http;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.zbus.kit.ConfigKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Broker;
import io.zbus.proxy.http.ProxyConfig.ProxyEntry;

/**
 * HttpProxy works like Nginx/Apache's proxy at first sight, but the
 * underlying traffic is totally different: Nginx/Apache actively connect to
 * target server, HttpProxy is always deployed on the side of
 * target server, and actively connect to zbus broker. 
 * 
 * The underlying network environment is usually called DMZ(DeMilitarized Zone).
 * 
 * @author rushmore (洪磊明)
 *
 */

public class HttpProxy implements Closeable {
	protected static final Logger log = LoggerFactory.getLogger(HttpProxy.class);  
	private Broker broker;
	private boolean ownBroker = false;  
	
	private Map<String, List<ProxyHandler>> entryHandlerTable = new HashMap<String, List<ProxyHandler>>();
	private ProxyConfig config;
	
	public HttpProxy(ProxyConfig config) throws IOException { 
		this.config = config;
		if (config.broker != null) {
			this.broker = config.broker;
		} else {
			String address = config.getBrokerAddress();
			if (address == null) {
				throw new IllegalArgumentException("Missing broker address");
			}
			this.broker = new Broker(); 
			String[] bb = address.split("[;, ]");
			for(String tracker : bb){
				if(tracker.equals("")) continue;
				this.broker.addTracker(tracker);
			} 
			this.ownBroker = true;
		}   
	}

	public synchronized void start() throws IOException {
		 for(Entry<String, ProxyEntry> e : this.config.getEntryTable().entrySet()){
			 String entryName = e.getKey();
			 ProxyEntry entry = e.getValue();
			 List<ProxyHandler> handlers = new ArrayList<ProxyHandler>();
			 for(String target : entry.targetList){
				 ProxyHandler handler = new ProxyHandler(entryName, target,
						 broker, config.getConnectionCount());
				 handlers.add(handler);
				 try{
					 handler.start(); 
				 } catch (Exception ex) { 
					 log.error(ex.getMessage(), ex);
				 }
			 }
			 entryHandlerTable.put(entryName, handlers);
		 }
	}
 
	 
	@Override
	public void close() throws IOException { 
		for(List<ProxyHandler> handlers : entryHandlerTable.values()){
			for(ProxyHandler handler : handlers){
				try{
					handler.close();
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		this.entryHandlerTable.clear();
		
		if (ownBroker && this.broker != null) {
			this.broker.close();
			this.broker = null;
		}
	} 
	

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		String configFile = ConfigKit.option(args, "-conf", "conf/http_proxy.xml"); 
		ProxyConfig config = new ProxyConfig();
		config.loadFromXml(configFile);
		  
		HttpProxy proxy = new HttpProxy(config);
		proxy.start();
	}
}
