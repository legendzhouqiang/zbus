package org.zbus.pool.impl;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.zbus.pool.ObjectFactory;
import org.zbus.pool.Pool;
import org.zbus.pool.PoolConfig;

public class DefaultPool<T> extends Pool<T> {
	private ObjectFactory<T> factory;
	private PoolConfig config;
	
	private BlockingQueue<T> queue = null;
	private final int maxTotal;
	private final AtomicInteger activeCount = new AtomicInteger(0);
	
	public DefaultPool(ObjectFactory<T> factory, PoolConfig config) { 
		this.factory = factory;
		this.config = config;
		
		this.maxTotal = this.config.getMaxTotal();
		this.queue = new ArrayBlockingQueue<T>(maxTotal);
	}
	
	@Override
	public void close() throws IOException { 
		T obj = null;
		while((obj = queue.poll()) != null){
			factory.destroyObject(obj);
		}
	}

	@Override
	public T borrowObject() throws Exception { 
		T  obj = null;
		if(activeCount.get() >= maxTotal){
			obj = queue.take();
			return obj;
		}
		obj = queue.poll();
		if(obj != null) return obj;
		
		obj = factory.createObject();
		activeCount.incrementAndGet(); 
		
		return obj; 
	}

	@Override
	public void returnObject(T obj) { 
		if(!factory.validateObject(obj)){
			activeCount.decrementAndGet();
			factory.destroyObject(obj); 
			return;
		}
		queue.offer(obj);
	}
}
