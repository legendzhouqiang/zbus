package org.zbus.mq.disk;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
 
class Block implements Closeable {  
	private final Index index; 
	private final int blockNumber; 
	
	private RandomAccessFile diskFile; 
	private final Lock lock = new ReentrantLock();  
	
	Block(Index index, File file, int blockNumber) throws IOException{   
		this.index = index;
		this.blockNumber = blockNumber;
		if(this.blockNumber < 0){
			throw new IllegalArgumentException("blockNumber should>=0 but was " + blockNumber);
		}
		if(this.blockNumber >= index.getBlockCount()){
			throw new IllegalArgumentException("blockNumber should<"+index.getBlockCount() + " but was " + blockNumber);
		}
		
		if(!file.exists()){
			File dir = file.getParentFile();
			if(!dir.exists()){
				dir.mkdirs();
			}  
		}  
		
		this.diskFile = new RandomAccessFile(file,"rw");   
	}   
	
	private static int HEAD_SIZE_FIXED = 41;
	public int write(DiskMessage data) throws IOException{
		try{
			lock.lock();
			
			int endOffset = endOffset();
			if(endOffset >= Index.BlockMaxSize){
				return 0;
			} 
			
			int headSize = HEAD_SIZE_FIXED;
			
			diskFile.seek(endOffset);
			diskFile.writeLong(endOffset);
			if(data.timestamp == null){
				diskFile.writeLong(System.currentTimeMillis()); 
			} else {
				diskFile.writeLong(data.timestamp);
			}
			if(data.id != null){
				diskFile.writeLong(data.id.getMostSignificantBits());
				diskFile.writeLong(data.id.getLeastSignificantBits());
			} else {
				diskFile.writeLong(0);
				diskFile.writeLong(0);
			}
			diskFile.writeLong(data.corrOffset==null? 0 : data.corrOffset);
			if(data.tag == null){
				diskFile.writeByte(0);
			} else {
				headSize += data.tag.length();
				diskFile.writeByte(data.tag.length());
				diskFile.write(data.tag.getBytes());
			}
			diskFile.writeInt(data.body.length);
			diskFile.write(data.body);
			
			endOffset += headSize + data.body.length;   
			index.writeEndOffset(endOffset);
			
			index.newDataAvailable.get().countDown();
			index.newDataAvailable.set(new CountDownLatch(1));
			
			return headSize + 4 + data.body.length;
		} finally {
			lock.unlock();
		}
	}
	
	private DiskMessage readHeadUnsafe(int pos) throws IOException{
    	DiskMessage data = new DiskMessage(); 
		
    	diskFile.seek(pos); 
		data.offset = diskFile.readLong(); //offset 
		data.timestamp = diskFile.readLong();
		long idHigh = diskFile.readLong();
		long idLow = diskFile.readLong();
		data.id = new UUID(idHigh, idLow);
		data.corrOffset = diskFile.readLong();
		byte tagLen = diskFile.readByte();
		if(tagLen > 0){
			byte[] tag = new byte[tagLen];
			diskFile.read(tag);
			data.tag = new String(tag);
		} 
		
		return data; 
	}
	
    public DiskMessage readHead(int pos) throws IOException{ 
    	try{
			lock.lock();
			return readHeadUnsafe(pos);
    	} finally {
			lock.unlock();
		}
	}
    
    public DiskMessage readFully(int pos) throws IOException{ 
    	try{
			lock.lock();
			DiskMessage data = readHeadUnsafe(pos);
			int size = diskFile.readInt();
			byte[] body = new byte[size];
			diskFile.read(body, 0, size);
			data.body = body;
			return data;
    	} finally {
			lock.unlock();
		}
    }
    
    public void readBody(DiskMessage head) throws IOException{ 
    	try{
			lock.lock();  
			int pos = HEAD_SIZE_FIXED + head.tag==null? 1 : head.tag.length();
			diskFile.seek(head.offset + pos); 
			int size = diskFile.readInt();
			byte[] body = new byte[size];
			diskFile.read(body, 0, size);
			head.body = body;
    	} finally {
			lock.unlock();
		}
	}
	
	public int write(byte[] data) throws IOException{
		try{
			lock.lock();
			
			int endOffset = endOffset();
			if(endOffset >= Index.BlockMaxSize){
				return 0;
			}
			diskFile.seek(endOffset);
			diskFile.writeLong(endOffset);
			diskFile.writeInt(data.length);
			diskFile.write(data);
			endOffset += 8 + 4 + data.length;  
			
			index.writeEndOffset(endOffset);
			
			index.newDataAvailable.get().countDown();
			index.newDataAvailable.set(new CountDownLatch(1));
			return data.length;
		} finally {
			lock.unlock();
		}
	}  

	
    public byte[] read(int pos) throws IOException{
    	try{
			lock.lock();
			diskFile.seek(pos); 
			diskFile.readLong(); //offset 
			int size = diskFile.readInt();
			byte[] data = new byte[size];
			diskFile.read(data, 0, size);
			return data;
    	} finally {
			lock.unlock();
		}
	}
    
    /**
     * Check if endOffset of block reached max block size allowed
     * @return true if max block size reached, false other wise
     * @throws IOException 
     */
    public boolean isFull() throws IOException{
    	return endOffset() >= Index.BlockMaxSize;
    }
    
    /**
     * Check if offset reached the end, for read.
     * @param offset offset of reading
     * @return true if reached the end of block(available data), false otherwise
     * @throws IOException 
     */
    public boolean isEndOfBlock(int offset) throws IOException{  
    	return offset >= endOffset();
    }
    
    private int endOffset() throws IOException{
    	return index.readOffset(blockNumber).endOffset;
    }
	
    public int getBlockNumber() {
		return blockNumber;
	}
    
	@Override
	public void close() throws IOException {  
		this.diskFile.close();
	} 
	
	public static class DiskMessage{
		public Long offset; //ignore when write
		public Long timestamp;
		public UUID id;
		public Long corrOffset; 
		public String tag;
		public byte[] body; 
	}
}
