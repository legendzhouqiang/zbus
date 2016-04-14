package org.zbus.net;

import java.io.Closeable;
import java.io.IOException;

public interface Session extends Closeable {
	String id(); 
	
	String getRemoteAddress();
	
	String getLocalAddress();
	
	void write(Object msg);
	
	void writeAndFlush(Object msg);
	
	void flush();
	
	boolean isActive();
	
	void asyncClose() throws IOException;
	
	<V> V attr(String key);
	
	<V> void attr(String key, V value);
}
