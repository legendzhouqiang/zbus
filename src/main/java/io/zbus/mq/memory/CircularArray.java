package io.zbus.mq.memory;

import java.util.ArrayList;
import java.util.List;

import io.zbus.mq.model.Channel;

public class CircularArray {
	private final int maxSize;
	private long start = 0; // readable entry
	private long end = 0;   // first entry to write
	private Object[] array;

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

	public Reader createReader(Channel channel) {
		return new Reader(channel);
	}

	public class Reader { 
		private final Channel channel; 
		Reader(Channel channel) {
			this.channel = channel; 
			if (this.channel.offset < start) {
				this.channel.offset = start;
			}
			if (this.channel.offset > end) {
				this.channel.offset = end;
			}
		}
		
		public void setOffset(long offset) {
			this.channel.offset = offset;
			if (this.channel.offset < start) {
				this.channel.offset = start;
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
	} 
}
