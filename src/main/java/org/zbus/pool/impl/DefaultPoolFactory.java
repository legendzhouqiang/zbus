package org.zbus.pool.impl;

import org.zbus.log.Logger;
import org.zbus.pool.ObjectFactory;
import org.zbus.pool.Pool;
import org.zbus.pool.PoolConfig;
import org.zbus.pool.PoolFactory;

public class DefaultPoolFactory implements PoolFactory {
	private static final Logger log = Logger.getLogger(Pool.class);
	@Override
	public <T> Pool<T> getPool(ObjectFactory<T> factory, PoolConfig config) { 
		log.info("Using Zbus DefaultPool");
		return new DefaultPool<T>(factory, config);
	}
}
