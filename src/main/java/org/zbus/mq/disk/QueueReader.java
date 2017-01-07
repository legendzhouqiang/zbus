package org.zbus.mq.disk;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class QueueReader extends MappedFile implements Comparable<QueueReader> {
	public static final int READER_FILE_SIZE = 256;  
	private Block block;  
	private final Index index;  
	private final String readerGroup; 
	
	private int blockNumber = 0;
	private int offset = 0;
	
	private final Lock readLock = new ReentrantLock();  
	
	public QueueReader(Index index, String readerGroup) throws IOException{
		this.index = index; 
		this.readerGroup = readerGroup; 
		File readerDir = new File(index.getIndexDir(), Index.ReaderDir);
		File file = new File(readerDir, this.readerGroup);
		
		load(file, READER_FILE_SIZE);  
		 
		block = this.index.createReadBlock(this.blockNumber);
	}   
	
	public QueueReader(QueueReader copy, String readerGroup) throws IOException{
		this.index = copy.index; 
		this.readerGroup = readerGroup; 
		File readerDir = new File(index.getIndexDir(), Index.ReaderDir);
		File file = new File(readerDir, this.readerGroup);
		 
		load(file, READER_FILE_SIZE); 
		this.blockNumber = copy.blockNumber;
		this.offset = copy.offset;
		
		block = this.index.createReadBlock(this.blockNumber);
	}   
	
	public boolean seek(long offset, String msgid) throws IOException{ 
		return true;
	}   
	
	public boolean seek(long time) throws IOException{ 
		return true;
	}  
	
	
	public boolean isEOF() throws IOException{
		readLock.lock();
		try{  
			if(block.isEndOfBlock(this.offset)){  
				if(this.blockNumber+1 >= index.getBlockCount()){
					return true;
				} 
			} 
			return false;
		} finally {
			readLock.unlock();
		} 
	} 
	
	private DiskMessage readUnsafe(String[] tagParts) throws IOException{
		if(block.isEndOfBlock(this.offset)){ 
			this.blockNumber++;
			if(this.blockNumber >= index.getBlockCount()){
				return null;
			}
			block = this.index.createReadBlock(this.blockNumber);
			this.offset = 0;
		}
		DiskMessage data = block.readFully(offset, tagParts);
		this.offset += data.bytesScanned;
		writeOffset(); 
		
		if(!data.valid){
			return readUnsafe(tagParts);
		}
		return data;
	}
	
	public DiskMessage read(String[] tagParts) throws IOException{
		readLock.lock();
		try{  
			return readUnsafe(tagParts);
		} finally {
			readLock.unlock();
		} 
	} 
	
	public DiskMessage read() throws IOException{
		return read(null);
	} 
	
	
	@Override
	protected void loadDefaultData() throws IOException {
		buffer.position(0);
		this.blockNumber = buffer.getInt();
		this.offset = buffer.getInt();
	}
	
	@Override
	protected void writeDefaultData() throws IOException {
		this.blockNumber = 0;
		this.offset = 0;
		
		writeOffset();
	}   
	
	public int getBlockNumber() {
		return blockNumber;
	}

	public int getOffset() {
		return offset;
	}

	private void writeOffset(){
		buffer.position(0); 
		buffer.putInt(blockNumber);
		
		buffer.position(4); 
		buffer.putInt(offset);
	}

	@Override
	public int compareTo(QueueReader o) { 
		if(this.blockNumber < o.blockNumber) return -1;
		if(this.blockNumber > o.blockNumber) return 1;
		return this.offset-o.offset;
	}
} 
