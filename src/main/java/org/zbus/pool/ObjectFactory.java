package org.zbus.pool;

public interface ObjectFactory<T> {
	
	T createObject() throws Exception;
	
	void destroyObject(T obj);
	
	boolean validateObject(T obj);
}
