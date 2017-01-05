package org.zbus.mq.disk;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
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
	
	public int write(DiskMessage data) throws IOException{
		try{
			lock.lock();
			
			int endOffset = endOffset();
			if(endOffset >= Index.BlockMaxSize){
				return 0;
			} 
			
			int size = 0;
			
			diskFile.seek(endOffset);
			diskFile.writeLong(endOffset);
			size += 8;
			if(data.timestamp == null){
				diskFile.writeLong(System.currentTimeMillis()); 
			} else {
				diskFile.writeLong(data.timestamp);
			}
			size += 8;
			if(data.id != null){
				diskFile.writeByte(data.id.length());
				diskFile.write(data.id.getBytes()); 
			} else {
				diskFile.writeByte(0); 
			}
			size += 40;
			diskFile.seek(endOffset + size); 
			diskFile.writeLong(data.corrOffset==null? 0 : data.corrOffset);
			size += 8;
			if(data.tag == null){
				diskFile.writeByte(0);
			} else { 
				diskFile.writeByte(data.tag.length());
				diskFile.write(data.tag.getBytes());
			}
			size += 128; 
			diskFile.seek(endOffset + size); 
			if(data.body != null){
				diskFile.writeInt(data.body.length);
				diskFile.write(data.body);
				size += 4 + data.body.length;
			} else {
				diskFile.writeInt(0); 
				size += 4;
			}
			
			endOffset += size;   
			index.writeEndOffset(endOffset);
			
			index.newDataAvailable.get().countDown();
			index.newDataAvailable.set(new CountDownLatch(1));
			
			return size;
		} finally {
			lock.unlock();
		}
	}
	
	private DiskMessage readHeadUnsafe(int pos) throws IOException{
    	DiskMessage data = new DiskMessage(); 
		
    	diskFile.seek(pos); 
    	int size = 0;
		data.offset = diskFile.readLong(); //offset 
		size += 8;
		data.timestamp = diskFile.readLong();
		size += 8;
		int idLen = diskFile.readByte();
		byte[] id = new byte[idLen];
		diskFile.read(id);
		data.id = new String(id); //!!<=39
		size += 40;
		
		diskFile.seek(pos + size); 
		data.corrOffset = diskFile.readLong();
		size += 8;
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
			diskFile.seek(pos + DiskMessage.BODY_POS);
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
			diskFile.seek(head.offset + DiskMessage.BODY_POS);
			int size = diskFile.readInt();
			byte[] body = new byte[size];
			diskFile.read(body, 0, size);
			head.body = body;
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
}
