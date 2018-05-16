package io.zbus.rpc.http;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.netty.handler.ssl.SslContext;
import io.zbus.kit.ClassKit;
import io.zbus.net.EventLoop;
import io.zbus.net.Ssl;
import io.zbus.net.http.HttpWsServer;
import io.zbus.rpc.Remote;
import io.zbus.rpc.RpcFilter;
import io.zbus.rpc.RpcProcessor; 
 

public class ServiceBootstrap implements Closeable {  
	private RpcProcessor processor = new RpcProcessor(); 
	private boolean autoDiscover = false;
	private int port;
	private String host = "0.0.0.0";
	private String certFile;
	private String keyFile;
	private HttpWsServer server;  
	private EventLoop eventLoop;     
	
	public ServiceBootstrap setPort(int port){ 
		this.port = port;
		return this;
	} 
	 
	public ServiceBootstrap setHost(String host){ 
		this.host = host;
		return this;
	}    
	
	public ServiceBootstrap setCertFile(String certFile){ 
		this.certFile = certFile; 
		return this;
	}  
	
	public ServiceBootstrap setKeyFile(String keyFile){ 
		this.keyFile = keyFile;
		return this;
	}  
	 
	public ServiceBootstrap setAutoDiscover(boolean autoDiscover) {
		this.autoDiscover = autoDiscover;
		return this;
	}  
	
	public ServiceBootstrap setStackTraceEnabled(boolean stackTrace) {
		this.processor.setStackTraceEnabled(stackTrace);
		return this;
	} 
	
	public ServiceBootstrap setMethodPageEnabled(boolean methodPage) {
		this.processor.setMethodPageEnabled(methodPage);
		return this;
	}  
	
	public ServiceBootstrap setMethodPageModule(String monitorModuleName) {
		this.processor.setMethodPageModule(monitorModuleName);
		return this;
	}  
	
	
	public void setBeforeFilter(RpcFilter beforeFilter) {
		this.processor.setBeforeFilter(beforeFilter);
	}

	public void setAfterFilter(RpcFilter afterFilter) {
		this.processor.setAfterFilter(afterFilter);
	}

	public void setAuthFilter(RpcFilter authFilter) {
		this.processor.setAuthFilter(authFilter);
	}

	private void validate(){ 
		
	}
	
	protected void initProcessor(){  
		Set<Class<?>> classes = ClassKit.scan(Remote.class);
		for(Class<?> clazz : classes){
			processor.addModule(clazz);
		}   
	}
	
	public RpcProcessor processor() {
		return this.processor;
	}
	 
	public ServiceBootstrap start() throws Exception{
		validate();  
		
		if(autoDiscover){
			initProcessor();
		} 
		if(processor.isMethodPageEnabled()) {
			processor.enableMethodPageModule();
		}
		
		eventLoop = new EventLoop();
		if(keyFile != null && certFile != null) {
			SslContext context = Ssl.buildServerSsl(certFile, keyFile);
			eventLoop.setSslContext(context);
		}
		
		server = new HttpWsServer(eventLoop);    
		RpcServerAdaptor adaptor = new RpcServerAdaptor(this.processor); 
		server.start(this.host, this.port, adaptor); 
		
		return this;
	}  
	
	public ServiceBootstrap addModule(Class<?>... clazz){
		processor.addModule(clazz);
		return this;
	}  
	
	public ServiceBootstrap addModule(String module, Class<?>... clazz){
		processor.addModule(module, clazz);
		return this;
	}
	
	public ServiceBootstrap addModule(String module, Object... services){
		processor.addModule(module, services);
		return this;
	}
	
	public ServiceBootstrap addModule(Object... services){
		processor.addModule(services);
		return this;
	}
	
	public void setModuleTable(Map<String, Object> instances){
		if(instances == null) return;
		for(Entry<String, Object> e : instances.entrySet()){
			processor.addModule(e.getKey(), e.getValue());
		}
	}
	
	
	@Override
	public void close() throws IOException {  
		if(server != null) {
			server.close();
		}
		if(eventLoop != null) {
			eventLoop.close();
		}
	}   
}
