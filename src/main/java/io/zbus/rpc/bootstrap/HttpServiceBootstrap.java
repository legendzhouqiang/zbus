package io.zbus.rpc.bootstrap;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import io.netty.handler.ssl.SslContext;
import io.zbus.kit.ClassKit;
import io.zbus.rpc.Remote;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.transport.http.RpcMessageAdaptor;
import io.zbus.transport.EventLoop;
import io.zbus.transport.SslKit;
import io.zbus.transport.http.MessageServer;
 

public class HttpServiceBootstrap implements Closeable {  
	protected RpcProcessor processor = new RpcProcessor(); 
	protected boolean autoDiscover = false;
	protected int port;
	protected String host = "0.0.0.0";
	protected String certFile;
	protected String keyFile;
	protected MessageServer server; 
	protected String token;
	protected EventLoop eventLoop;
	 
	public HttpServiceBootstrap port(int port){ 
		this.port = port;
		return this;
	} 
	 
	public HttpServiceBootstrap host(String host){ 
		this.host = host;
		return this;
	}   
	
	public HttpServiceBootstrap ssl(String certFile, String keyFile){ 
		this.certFile = certFile;
		this.keyFile = keyFile;
		return this;
	}  
	 
	public HttpServiceBootstrap autoDiscover(boolean autoDiscover) {
		this.autoDiscover = autoDiscover;
		return this;
	} 
	
	public HttpServiceBootstrap serviceToken(String token){ 
		this.token = token;
		return this;
	}  
	
	private void validate(){ 
		
	}
	
	protected void initProcessor(){  
		Set<Class<?>> classes = ClassKit.scan(Remote.class);
		for(Class<?> clazz : classes){
			processor.addModule(clazz);
		}  
	}
	 
	public HttpServiceBootstrap start() throws Exception{
		validate();  
		
		if(autoDiscover){
			initProcessor();
		} 
		eventLoop = new EventLoop();
		if(keyFile != null && certFile != null) {
			SslContext context = SslKit.buildServerSsl(certFile, keyFile);
			eventLoop.setSslContext(context);
		}
		
		server = new MessageServer(eventLoop);   
		RpcMessageAdaptor adaptor = new RpcMessageAdaptor(this.processor);
		adaptor.setToken(this.token);
		server.start(this.host, this.port, adaptor); 
		
		return this;
	}  
	
	public HttpServiceBootstrap addModule(Class<?>... clazz){
		processor.addModule(clazz);
		return this;
	}  
	
	public HttpServiceBootstrap addModule(String module, Object... services){
		processor.addModule(module, services);
		return this;
	}
	
	public HttpServiceBootstrap addModule(Object... services){
		processor.addModule(services);
		return this;
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
