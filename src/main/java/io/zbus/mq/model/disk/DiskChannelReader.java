package io.zbus.mq.model.disk;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.mq.model.Channel;
import io.zbus.mq.model.ChannelReader;
import io.zbus.mq.model.disk.support.DiskMessage;
import io.zbus.mq.model.disk.support.QueueReader;

public class DiskChannelReader implements ChannelReader { 
	private static final Logger logger = LoggerFactory.getLogger(DiskChannelReader.class); 
	
	private final QueueReader reader;  
	private final String name;
	public DiskChannelReader(String channel, DiskQueue diskQueue) throws IOException{  
		this.name = channel;
		reader = new QueueReader(diskQueue.index, channel);   
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
	 
	
	public boolean seek(Long totalOffset, String msgid) throws IOException{ 
		return reader.seek(totalOffset, msgid);
	}
	
	@Override
	public String getFilter() { 
		return reader.getFilter();
	}
	
	public void setFilter(String filter) {
		reader.setFilter(filter);
	} 
	
	@Override
	public void close() throws IOException {
		reader.close();  
	} 
	 
	
	public Integer getMask(){
		return reader.getMask(); 
	}
	
	public void setMask(Integer mask){
		reader.setMask(mask);  
	}  
	
	@Override
	public Channel channel() {
		Channel channel = new Channel(this.name);
		channel.mask = getMask();
		channel.offset = (long) reader.getOffset(); //TODO
		return channel;
	}

	@Override
	public void destroy() { 
		try {
			reader.delete();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
}