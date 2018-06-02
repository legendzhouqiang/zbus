package io.zbus.mq.memory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.zbus.mq.Protocol;
import io.zbus.mq.model.Channel;
import io.zbus.mq.model.ChannelReader;

public class MemoryChannelReader implements ChannelReader {  
	private final CircularArray queue;
	private final Channel channel; 
	private String filter;
	private Integer mask;
	
	public MemoryChannelReader(CircularArray circularArray, Channel channel) {
		queue = circularArray;
		this.channel = channel.clone(); 
		if (this.channel.offset == null || this.channel.offset < queue.start) {
			this.channel.offset = queue.end;
		}
		if (this.channel.offset > queue.end) {
			this.channel.offset = queue.end;
		}
	}  

	public int size() {
		synchronized (queue.array) {
			if (channel.offset < queue.start) {
				channel.offset = queue.start;
			}
			return (int) (queue.end - channel.offset);
		}
	}

	@Override
	public void close() throws IOException { 
		
	}
 
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> read(int count) throws IOException { 
		synchronized (queue.array) {
			List<Map<String, Object>> res = new ArrayList<>();
			if (channel.offset < queue.start) {
				channel.offset = queue.start;
			}
			int c = 0;
			while (channel.offset < queue.end) {
				int idx = (int) (channel.offset % queue.maxSize);
				Map<String, Object> data = (Map<String, Object>)queue.array[idx];
				data.put(Protocol.OFFSET, channel.offset); //Add offset
				res.add(data);
				channel.offset++;
				c++;
				if(c >= count) break; 
			}
			return res;
		} 
	} 
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> read() throws IOException {
		synchronized (queue.array) { 
			if (channel.offset < queue.start) {
				channel.offset = queue.start;
			} 
			Map<String, Object> res = null;
			if (channel.offset < queue.end) {
				int idx = (int) (channel.offset % queue.maxSize);
				res = (Map<String, Object>)queue.array[idx];
				res.put(Protocol.OFFSET, channel.offset); //Add offset
				channel.offset++; 
			}
			return res;
		} 
	}

	@Override
	public boolean seek(Long offset, String msgid) throws IOException {
		if(offset == null) return false;
		this.channel.offset = offset;
		if (this.channel.offset < queue.start) {
			this.channel.offset = queue.start;
		}
		return true; 
	}

	@Override
	public void destroy() {  
		
	}

	@Override
	public boolean isEnd() {
		return size() <= 0;
	}

	@Override
	public void setFilter(String filter) { 
		this.filter = filter;
	}
	
	public String getFilter() {
		return filter;
	}

	@Override
	public Integer getMask() { 
		return mask;
	}

	@Override
	public void setMask(Integer mask) {
		this.mask = mask;
	}

	@Override
	public Channel channel() { 
		return channel.clone();
	}
}