package io.zbus.mq.memory;

import java.util.ArrayList;
import java.util.List;

public class CircleQueue {
	private final int maxSize;
	private long start = 0; // readable entry
	private long end = 0;   // first entry to write
	private Object[] array;

	public CircleQueue(int maxSize) {
		this.maxSize = maxSize;
		array = new Object[this.maxSize];
	}

	public CircleQueue() {
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

	public Reader createReader() {
		return new Reader();
	}

	public Reader createReader(long offset) {
		return new Reader(offset);
	}

	public class Reader {
		private long offset = start;

		public Reader() {

		}

		public Reader(long offset) {
			this.offset = offset;
			if (this.offset < start) {
				this.offset = start;
			}
			if (this.offset > end) {
				this.offset = end;
			}
		}

		@SuppressWarnings("unchecked")
		public <T> List<T> read(int batchSize) {
			synchronized (array) {
				List<T> res = new ArrayList<>();
				if (offset < start) {
					offset = start;
				}
				while (offset < end) {
					int idx = (int) (offset % maxSize);
					res.add((T) array[idx]);
					offset++;
				}
				return res;
			}
		}

		public int size() {
			synchronized (array) {
				if (offset < start) {
					offset = start;
				}
				return (int) (end - offset);
			}
		}
	}

	public static void main(String[] args) {
		CircleQueue q = new CircleQueue();
		q.write("abc", "efg");
		q.write("xyz");

		Reader reader = q.createReader();
		System.out.println(reader.size());
		List<String> res = reader.read(20);
		for (String s : res) {
			System.out.println(s);
		}
		System.out.println(reader.size());
	}

}
