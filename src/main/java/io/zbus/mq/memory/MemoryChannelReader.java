package io.zbus.mq.memory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	@SuppressWarnings("unchecked")
	public <T> List<T> read(int count) {
		synchronized (queue.array) {
			List<T> res = new ArrayList<>();
			if (channel.offset < queue.start) {
				channel.offset = queue.start;
			}
			int c = 0;
			while (channel.offset < queue.end) {
				int idx = (int) (channel.offset % queue.maxSize);
				res.add((T) queue.array[idx]);
				channel.offset++;
				c++;
				if(c > count) break; 
			}
			return res;
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

	@Override
	public Map<String, Object> read() throws IOException { 
		return null;
	}

	@Override
	public Map<String, Object> read(long offset) throws IOException {
		return null;
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