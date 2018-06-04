package io.zbus.transport.inproc;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.transport.Invoker.AbstractInvoker;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Session;

public class InprocClient extends AbstractInvoker implements Session { 
	private static final Logger logger = LoggerFactory.getLogger(InprocClient.class); 
	private ConcurrentMap<String, Object> attributes = null;
	
	private IoAdaptor ioAdaptor;
	private final String id = StrKit.uuid();
	private boolean active = true;
	
	public InprocClient(IoAdaptor ioAdaptor) {
		this.ioAdaptor = ioAdaptor; 
		try {
			ioAdaptor.sessionCreated(this);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void close() throws IOException { 
		active = false;
		ioAdaptor.sessionToDestroy(this); 
	} 

	@Override
	public String id() { 
		return id;
	}

	@Override
	public String remoteAddress() { 
		return "InprocServer";
	}

	@Override
	public String localAddress() {
		return "Inproc-"+id;
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public void write(Object msg) {  //Session received message
		try {
			Map<String, Object> data = null;
			if(msg instanceof Map) {
				data = (Map<String, Object>)msg; 
			} else if(msg instanceof byte[]) {
				data = JsonKit.parseObject((byte[])msg);
			} else if(msg instanceof String) {
				data = JsonKit.parseObject((String)msg);
			} else {
				throw new IllegalArgumentException("type of msg not support: " + msg.getClass());
			}
			onResponse(data);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void sendMessage(Map<String, Object> data) {  //Session send out message
		try {
			ioAdaptor.onMessage(data, this);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean active() { 
		return active;
	}  
	
	@SuppressWarnings("unchecked")
	public <V> V attr(String key) {
		if (this.attributes == null) {
			return null;
		}

		return (V) this.attributes.get(key);
	}

	public <V> void attr(String key, V value) {
		if(value == null){
			if(this.attributes != null){
				this.attributes.remove(key);
			}
			return;
		}
		if (this.attributes == null) {
			synchronized (this) {
				if (this.attributes == null) {
					this.attributes = new ConcurrentHashMap<String, Object>();
				}
			}
		}
		this.attributes.put(key, value);
	} 
	
	@Override
	public String toString() { 
		return localAddress();
	}
}
