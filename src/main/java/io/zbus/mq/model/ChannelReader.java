package io.zbus.mq.model;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public interface ChannelReader extends Closeable  {  
	
	Map<String, Object> read() throws IOException; 
	
	Map<String, Object> read(long offset) throws IOException;
	
	boolean seek(Long offset, String msgid) throws IOException; 
	
	void destroy();
	
	boolean isEnd(); 

	void setFilter(String filter);   
	
	String getFilter();
	
	Integer getMask(); 
	
	void setMask(Integer mask);
	
	Channel channel();
}