package io.zbus.rpc.http;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslContext;
import io.zbus.kit.ClassKit;
import io.zbus.rpc.GenericInvocation;
import io.zbus.rpc.RegisterInterceptor;
import io.zbus.rpc.RpcFilter;
import io.zbus.rpc.RpcMethod;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.annotation.Remote;
import io.zbus.transport.Ssl;
import io.zbus.transport.http.HttpWsServer; 
 

public class RpcBootstrap implements Closeable {  
	private static final Logger log = LoggerFactory.getLogger(RpcBootstrap.class);
	
	private RpcProcessor processor = new RpcProcessor(); 
	private boolean autoLoadService = false;
	private int port;
	private String host = "0.0.0.0";
	private String certFile;
	private String keyFile;
	private HttpWsServer server;    
	private RegisterInterceptor onStart;
	
	public RpcBootstrap setPort(int port){ 
		this.port = port;
		return this;
	} 
	 
	public RpcBootstrap setHost(String host){ 
		this.host = host;
		return this;
	}    
	
	public RpcBootstrap setCertFile(String certFile){ 
		this.certFile = certFile; 
		return this;
	}  
	
	public RpcBootstrap setKeyFile(String keyFile){ 
		this.keyFile = keyFile;
		return this;
	}  
	 
	public RpcBootstrap setAutoLoadService(boolean autoLoadService) {
		this.autoLoadService = autoLoadService;
		return this;
	}  
	
	public RpcBootstrap setStackTraceEnabled(boolean stackTraceEnabled) {
		this.processor.setStackTraceEnabled(stackTraceEnabled);
		return this;
	} 
	
	public RpcBootstrap setMethodPageEnabled(boolean methodPageEnabled) {
		this.processor.setMethodPageEnabled(methodPageEnabled);
		return this;
	}  
	
	public RpcBootstrap setMethodPageAuthEnabled(boolean methodPageAuthEnabled) {
		this.processor.setMethodPageAuthEnabled(methodPageAuthEnabled);
		return this;
	}
	
	public RpcBootstrap setMethodPageModule(String monitorModuleName) {
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
	
	public void setOnStart(RegisterInterceptor onStart) {
		this.onStart = onStart;
	}

	private void validate(){ 
		
	}
	
	protected void initProcessor(){  
		Set<Class<?>> classes = ClassKit.scan(Remote.class);
		for(Class<?> clazz : classes){ 
			try {
				processor.addModule(clazz.newInstance());
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
			} 
		}   
	}
	
	public RpcProcessor processor() {
		return this.processor;
	}
	 
	public RpcBootstrap start() throws Exception{
		validate();  
		
		if(autoLoadService){
			initProcessor();
		} 
		if(processor.isMethodPageEnabled()) {
			processor.enableMethodPageModule();
		}
		
		if(onStart != null) {
			onStart.onStart(processor);
		}
		
		server = new HttpWsServer();    
		if(keyFile != null && certFile != null) {
			SslContext context = Ssl.buildServerSsl(certFile, keyFile);
			server.setSslContext(context);
		}  
		
		RpcServerAdaptor adaptor = new RpcServerAdaptor(this.processor); 
		server.start(this.host, this.port, adaptor); 
		
		return this;
	}   
	 
	public RpcBootstrap addModule(Object service){
		processor.addModule(service);
		return this;
	}
	
	public RpcBootstrap addModule(String module, Class<?> service){
		try {
			Object obj = service.newInstance();
			processor.addModule(module, obj);
		} catch (InstantiationException | IllegalAccessException e) {
			log.error(e.getMessage(),e);
		} 
		return this;
	}
	
	public RpcBootstrap addModule(String module, Object service){
		processor.addModule(module, service);
		return this;
	}
	
	public RpcBootstrap addModule(List<Object> services){
		for(Object svc : services) {
			processor.addModule(svc);
		}
		return this;
	}
	
	public RpcBootstrap addModule(String module, List<Object> services){
		for(Object svc : services) {
			processor.addModule(module, svc);
		}
		return this;
	}  
	
	
	@SuppressWarnings("unchecked")
	public void setModuleTable(Map<String, Object> instances){
		if(instances == null) return;
		for(Entry<String, Object> e : instances.entrySet()){
			Object svc = e.getValue();
			if(svc instanceof List) {
				addModule(e.getKey(), (List<Object>)svc); 
			} else {
				addModule(e.getKey(), svc);
			}
		}
	}
	
	public RpcBootstrap addMethod(RpcMethod spec, GenericInvocation genericInvocation){
		processor.addMethod(spec, genericInvocation);
		return this;
	}  
	
	public RpcBootstrap removeMethod(String module, String method){
		processor.removeMethod(module, method);
		return this;
	}  
	
	@Override
	public void close() throws IOException {  
		if(server != null) {
			server.close();
		} 
	}   
}
