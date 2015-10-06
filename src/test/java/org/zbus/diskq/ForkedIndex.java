package org.zbus.diskq;

import java.nio.MappedByteBuffer;

public class ForkedIndex{
	public static final int SIZE = 100;
	public static final int FLAG_OFFSET = 0;
	public static final int NUM_OFFSET = 1;
	public static final int POS_OFFSET = 9;
	public static final int CNT_OFFSET = 13;
	public static final int ID_OFFSET = 21;

	private final int OFFSET;
	private final int index;
	private final MappedByteBuffer mappedBuffer;

	private volatile byte flag; // 标识是否启用
	private volatile long readNum;
	private volatile int readPos;
	private volatile long readCount;
	private volatile String forkId;

	public ForkedIndex(int index, MappedByteBuffer mappedBuffer) {
		this.index = index;
		OFFSET = this.index * SIZE + DiskQueueFork.FORK_OFFSET;
		this.mappedBuffer = (MappedByteBuffer) mappedBuffer.duplicate();
		
		
	}
	
	public void load(){
		mappedBuffer.position(OFFSET);
		this.flag = this.mappedBuffer.get(); 
		this.readNum = this.mappedBuffer.getLong();
		this.readPos = this.mappedBuffer.getInt();
		this.readCount = this.mappedBuffer.getLong();
		
		int len = this.mappedBuffer.get(); //21
		if(len > 0 && len <=78){
			byte[] data = new byte[len];
			this.mappedBuffer.get(data);
			this.forkId = new String(data);
		}
	}
	
	public static boolean isActive(int value){
		return (value & 1) != 0;
	}
	
	public boolean isActive(){
		return (this.flag & 1) != 0;
	}

	public void putFlag(byte flag) {
		mappedBuffer.position(OFFSET + FLAG_OFFSET);
		mappedBuffer.put(flag);
		this.flag = flag;
	}
	
	public void putReadNum(long readNum) {
		mappedBuffer.position(OFFSET + NUM_OFFSET);
		mappedBuffer.putLong(readNum);
		this.readNum = readNum;
	}
	
	public void putReadPos(int readPos) {
		mappedBuffer.position(OFFSET + POS_OFFSET);
		mappedBuffer.putInt(readPos);
		this.readPos = readPos;
	}
	
	public void putReadCount(long readCount) {
		mappedBuffer.position(OFFSET + CNT_OFFSET);
		mappedBuffer.putLong(readCount);
		this.readNum = readCount;
	}
	
	public void putForkId(String forkId) { 
		byte[] forkIdBytes = forkId.getBytes();
		if(forkIdBytes.length > 78){
			throw new IllegalArgumentException(forkId + " too long");
		}
		mappedBuffer.position(OFFSET + ID_OFFSET);
		mappedBuffer.put((byte)forkIdBytes.length);
		mappedBuffer.put(forkIdBytes);
		
		this.forkId = forkId;
	}
	
	

    public byte getFlag() {
		return flag;
	}

	public long getReadNum() {
		return readNum;
	}

	public int getReadPos() {
		return readPos;
	}

	public long getReadCount() {
		return readCount;
	}

	public String getForkId() {
		return forkId;
	}

	public void sync() {
        if (mappedBuffer != null) {
        	mappedBuffer.force();
        }
    }
}