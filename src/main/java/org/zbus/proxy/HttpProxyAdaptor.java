package org.zbus.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.zbus.kit.ConfigKit;
import org.zbus.kit.FileKit;
import org.zbus.log.Logger;
import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageCodec;


public class HttpProxyAdaptor extends BindingAdaptor{ 
	private static final Logger log = Logger.getLogger(HttpProxyAdaptor.class);     
	
	private final Map<String, String> urlMap;  
	private static final DownStreamBindingAdaptor downstreamAdaptor = new DownStreamBindingAdaptor();

    public HttpProxyAdaptor(Map<String, String> urlMap){  
    	codec(new MessageCodec()); 
    	this.urlMap = Collections.synchronizedMap(urlMap); 
    } 
    
    @Override
    protected void onMessage(Object obj, Session sess) throws IOException {
    	Message msg = (Message)obj; 
    	String uri = msg.getUri(); 
    	int idx = uri.indexOf('/', 1);
    	if(idx == -1) idx = uri.indexOf('?');
    	String entry = uri.substring(1);
    	if(idx != -1){
    		entry = uri.substring(1, idx);
    		uri = uri.substring(idx);
    		msg.setUri(uri);
    	} else {
    		msg.setUri("/");
    	}
    	
    	
    	String targetAddress = urlMap.get(entry);
    	if(targetAddress == null){
    		msg.setStatus(404);
    		msg.setBody(entry + " Not Found"); 
    		sess.write(msg);
    		return;
    	}
    	
    	
    	if(sess.chain != null) {
    		if(targetAddress.equals(sess.chain.attr("target"))){
    			sess.chain.write(msg.toIoBuffer());
    			return;
    		} else { //same socket, but target changed
    			sess.chain.asyncClose(); //close downstream
    			sess.chain = null;
    		}
    	}
    	
    	//downstream not exist
    	sess.attr("msg", msg); 
    	
    	Dispatcher dispatcher = sess.getDispatcher();
    	Session target = null;
    	try{  
	    	target = dispatcher.createClientSession(targetAddress, downstreamAdaptor); 
	    	target.attr("target", targetAddress);
    	} catch (Exception e){ 
    		log.error("Reject upstream connection: %s", sess); 
    		sess.asyncClose();
    		return;
    	}
    	
    	sess.chain = target;
    	target.chain = sess; 
    	
    	dispatcher.registerSession(SelectionKey.OP_CONNECT, target);  
    	
    } 
   
    
    private static class DownStreamBindingAdaptor extends BindingAdaptor{
    	
    	DownStreamBindingAdaptor(){
    		codec(new MessageCodec());
    	}
    	
    	@Override
    	protected void onMessage(Object obj, Session sess) throws IOException {
    		Message msg = (Message)obj;
    		if(sess.chain != null){
    			sess.chain.write(msg);
    		} 
    		//just discard
    	}
    	
    	@Override
    	public void onSessionConnected(Session sess) throws IOException {
    		Session chain = sess.chain;
    		if(chain == null){
    			sess.asyncClose();
    			return; 
    		}
    		
    		Message msg = chain.attr("msg");
    		chain.removeAttr("msg");
    		sess.register(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    		if(msg != null){
    			sess.write(msg);
    		}
    	}
    }
    
    
	public static void main(String[] args) throws Exception {  
		String configFile = ConfigKit.option(args, "-conf", "http.properties");
		
		InputStream fis = FileKit.loadFile(configFile);
		if(fis == null){
			log.error("Missing HttpProxy config file: "+configFile);
			return;
		}
		Properties props = new Properties();
		props.load(fis);
		
		String serverHost = ConfigKit.value(props, "zbus.proxy.http.host", "0.0.0.0");
		int serverPort = ConfigKit.value(props, "zbus.proxy.http.port", 80);
		String serverAddress = serverHost+":"+serverPort;
		
		Map<String, String> urlMap = new HashMap<String, String>();
		Set<String> entries = ConfigKit.valueSet(props, "zbus.proxy.http");
		for(String name : entries){
			String key = String.format("zbus.proxy.http.%s.target",name);
			Set<String> targets = ConfigKit.valueSet(props, key); 
			if(targets.size() == 0){
				log.warn("Missing tareget for "+key);
				continue;
			} 
			
			String target = targets.iterator().next(); //TODO
			urlMap.put(name, target); 
			log.info("Http Proxy: %s=>%s", name, target);
		}
		
		
		Dispatcher dispatcher = new Dispatcher(); 
		IoAdaptor ioAdaptor = new HttpProxyAdaptor(urlMap);
		
		@SuppressWarnings("resource")
		Server server = new Server(dispatcher, ioAdaptor, serverAddress);
		server.setServerName("HttpProxyServer");
		server.start();
	}
}
