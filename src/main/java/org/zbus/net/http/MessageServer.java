package org.zbus.net.http;

import java.io.IOException;
import java.util.List;

import org.zbus.net.CodecInitializer;
import org.zbus.net.EventDriver;
import org.zbus.net.IoAdaptor;
import org.zbus.net.Server;
import org.zbus.net.netty.NettyServer;
import org.zbus.net.netty.http.MessageToHttpWsCodec;
import org.zbus.net.simple.DefaultServer;
import org.zbus.net.simple.http.MessageCodec;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class MessageServer implements Server {
	private Server support;  
	private EventDriver eventDriver;
	private boolean ownEventDriver = false;
	public MessageServer(){ 
		 this(null);
	}
	
	public MessageServer(EventDriver driver){  
		this.eventDriver = driver; 
		if(this.eventDriver == null){
			this.eventDriver = new EventDriver();
			this.ownEventDriver = true;
		} else {
			this.ownEventDriver = false;
		}
		
		if(eventDriver.isNettyEnabled()){
			support = new NettyServer(eventDriver);  
			support.codec(new CodecInitializer() { 
				@Override
				public void initPipeline(List<Object> p) { 
					p.add(new HttpServerCodec());
					p.add(new HttpObjectAggregator(1024*1024*10));
					p.add(new MessageToHttpWsCodec());
				}
			}); 
			
		} else { 
			support = new DefaultServer(eventDriver);
			support.codec(new CodecInitializer() { 
				@Override
				public void initPipeline(List<Object> p) { 
					p.add(new MessageCodec()); 
				}
			}); 
		} 
	}
	 

	@Override
	public void close() throws IOException { 
		support.close();
		if(ownEventDriver && eventDriver != null){
			eventDriver.close();
			eventDriver = null;
		}
	}

	@Override
	public void codec(CodecInitializer codecInitializer) {
		support.codec(codecInitializer);
	}

	@Override
	public void start(int port, IoAdaptor ioAdaptor) throws Exception {
		support.start(port, ioAdaptor);
	}

	@Override
	public void start(String host, int port, IoAdaptor ioAdaptor) throws Exception {
		support.start(host, port, ioAdaptor);
	}

	@Override
	public void join() throws InterruptedException {
		support.join();
	} 
	
	@Override
	public EventDriver getEventDriver() {
		return this.eventDriver;
	}
}
