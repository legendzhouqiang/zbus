package io.zbus.mq.model.memory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.ChannelReader;

public class CircularArray {
	private final int maxSize;
	private long start = 0; // readable entry
	private long end = 0;   // first entry to write
	Object[] array;

	public CircularArray(int maxSize) {
		this.maxSize = maxSize;
		array = new Object[this.maxSize];
	}

	public CircularArray() {
		this(10000);
	}

	public int size() {
		synchronized (array) {
			return (int) (end - start);
		}
	}

	private int forwardIndex() {
		if (end - start >= maxSize) {
			start++;
		}
		end++;
		return (int) (end % maxSize);
	}

	public void write(Object... data) {
		synchronized (array) {
			for (Object obj : data) {
				int i = (int) (end % maxSize);
				array[i] = obj;
				forwardIndex();
			}
		}
	} 

	public MemoryChannelReader createReader(Channel channel) {
		return new MemoryChannelReader(channel);
	}

	public class MemoryChannelReader implements ChannelReader { 
		private final Channel channel; 
		private String filter;
		private Integer mask;
		
		public MemoryChannelReader(Channel channel) {
			this.channel = channel; 
			if (this.channel.offset == null || this.channel.offset < start) {
				this.channel.offset = end;
			}
			if (this.channel.offset > end) {
				this.channel.offset = end;
			}
		} 

		@SuppressWarnings("unchecked")
		public <T> List<T> read(int count) {
			synchronized (array) {
				List<T> res = new ArrayList<>();
				if (channel.offset < start) {
					channel.offset = start;
				}
				int c = 0;
				while (channel.offset < end) {
					int idx = (int) (channel.offset % maxSize);
					res.add((T) array[idx]);
					channel.offset++;
					c++;
					if(c > count) break; 
				}
				return res;
			}
		}

		public int size() {
			synchronized (array) {
				if (channel.offset < start) {
					channel.offset = start;
				}
				return (int) (end - channel.offset);
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
			if (this.channel.offset < start) {
				this.channel.offset = start;
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
}
