package io.zbus.mq.model;

import java.util.List;

public class CircleQueue {
	private final int maxSize;
	private long readerIndex = -1;
	private long writeIndex = 0;
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
			return (int)(writeIndex-readerIndex-1);
		} 
	}
	
	private int nextWriteIndex() {
		synchronized (array) {
			writeIndex++;
			if(writeIndex-readerIndex-1>maxSize) {
				readerIndex++;
			}
			return (int)(writeIndex%maxSize);
		} 
	}
	
	public void write(Object... data) {
		for(Object obj : data) {
			int i = nextWriteIndex();
			array[i] = obj; 
		}
	}
	
	public <T> List<T> read(long offset, int batchSize){
		long idx = offset;
		if(idx < readerIndex) {
			idx = readerIndex;
		}
		if(idx + batchSize > writeIndex) {
			
		}
		
		return null;
	}
	
	public static void main(String[] args) { 
	}
	
}
