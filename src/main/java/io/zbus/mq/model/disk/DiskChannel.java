package io.zbus.mq.model.disk;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import io.zbus.kit.JsonKit;
import io.zbus.mq.model.Channel;
import io.zbus.mq.model.disk.support.DiskMessage;
import io.zbus.mq.model.disk.support.QueueReader;

public class DiskChannel extends Channel implements Closeable { 
	private final QueueReader reader;  
	
	public DiskChannel(String channel, DiskQueue diskQueue) throws IOException{  
		super(channel); 
		reader = new QueueReader(diskQueue.index, this.name);   
	}
	
	public DiskChannel(String channel, QueueReader reader) throws IOException{ 
		super(channel);
		this.reader = new QueueReader(reader, channel);  
	} 
	  
	public boolean isEnd() { 
		try {
			return reader.isEOF();
		} catch (IOException e) {
			return true;
		}
	} 
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> read() throws IOException { 
		DiskMessage data = reader.read();  
		if(data == null){
			return null;
		} 
		return  JsonKit.parseObject(new String(data.body, "UTF8"), Map.class);
	} 
	 
	@SuppressWarnings("unchecked")
	public Map<String, Object> read(long offset) throws IOException {
		DiskMessage data = reader.read(offset);
		if(data == null) {
			return null;
		}
		return  JsonKit.parseObject(new String(data.body, "UTF8"), Map.class);
	}
	 
	
	public boolean seek(long totalOffset, String msgid) throws IOException{ 
		return reader.seek(totalOffset, msgid);
	}
	
	public boolean seek(long time) throws IOException { 
		return reader.seek(time);
	}
	
	public void setFilter(String filter) {
		reader.setFilter(filter);
	} 
	
	@Override
	public void close() throws IOException {
		reader.close();  
	} 
	
	public void destory() throws IOException{ 
		reader.delete();
	}
	
	public Integer getMask(){
		return reader.getMask(); 
	}
	
	public void setMask(Integer mask){
		reader.setMask(mask); 
		this.mask = mask;
	}  
}