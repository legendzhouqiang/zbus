package org.zbus.pool;

import java.io.Closeable;

import org.zbus.pool.impl.DefaultPoolFactory;


public abstract class Pool<T> implements Closeable { 
	
	public abstract T borrowObject() throws Exception;
	
	public abstract void returnObject(T obj);

	public static <T> Pool<T> getPool(ObjectFactory<T> factory, PoolConfig config){
		return Pool.factory.getPool(factory, config);
	}
	
	
	private static PoolFactory factory;
	
	static {
		initDefaultFactory();
	} 
	
	public static void setPoolFactory(PoolFactory factory) {
		if (factory != null) {
			Pool.factory = factory;
		}
	}
	
	public static void initDefaultFactory() {
		if (factory != null){
			return ;
		}
		String defaultFactory = String.format("%s.impl.CommonsPool2Factory", Pool.class.getPackage().getName());
		try {
			//default to Log4j
			Class.forName("org.apache.commons.pool2.BasePooledObjectFactory");
			Class<?> factoryClass = Class.forName(defaultFactory);
			factory = (PoolFactory)factoryClass.newInstance();
		} catch (Exception e) { 
			factory = new DefaultPoolFactory();
		}
	}
}
