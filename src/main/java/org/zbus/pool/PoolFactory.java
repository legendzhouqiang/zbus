package org.zbus.pool;

public interface PoolFactory {
	<T> Pool<T> getPool(ObjectFactory<T> factory, PoolConfig config);
}
