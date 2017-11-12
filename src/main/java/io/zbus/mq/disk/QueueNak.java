package io.zbus.mq.disk;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
 
public class QueueNak extends MappedFile { 
	public static final int NakMaxCount = 1000;   
	private static final int FileSize = HeadSize + NakMaxCount * NakRecord.Size;
 
	private final File nakFile;
	 
	private static final int NakLimitPos = 0;  
	private Queue<NakRecord> nakQueue = new PriorityQueue<NakRecord>(new NakRecordComparator());
	private Queue<Integer> availableEntries = new PriorityQueue<Integer>(); 
	
	private int nakLimit = 100; 

	public QueueNak(File nakFile) throws IOException{
		this.nakFile = nakFile;
		load(this.nakFile, FileSize); 
	}   
	
	public int size() {
		return nakQueue.size();
	}
	
	public Iterator<NakRecord> iterator(){
		return nakQueue.iterator();
	}
	
	public void clear() {
		buffer.position(HeadSize);
		buffer.put(new byte[NakRecord.Size*NakMaxCount]);
		nakQueue.clear(); 
		initAvailableEntries();
	}
	
	private void initAvailableEntries() {
		for(int i=0;i<NakMaxCount;i++) {
			availableEntries.add(i);
		}
	}
	
	public NakRecord queryNak(long offset) {
		Iterator<NakRecord> iter = iterator();
		while(iter.hasNext()) {
			NakRecord nak = iter.next();
			if(nak.offset == offset) {
				return nak;
			}
		}
		return null;
	} 
	
	public void addNak(long offset, String msgId) {
		try {
			lock.lock(); 
			if(msgId == null) {
				throw new IllegalArgumentException("msgId is null");
			}
			
			NakRecord nak = queryNak(offset);
			if(nak != null) {
				if(!msgId.equals(nak.msgId)) {
					throw new IllegalStateException("msgId not matched");
				}
				nak.retryCount++;
				nak.updatedTime = System.currentTimeMillis();
				writeNakUnsafe(nak);
				return;
			}
			
			if(nakQueue.size() > nakLimit) {
				throw new IllegalStateException("NAK queue full");
			}
			nak = new NakRecord(); 
			Integer entryNumber = availableEntries.poll();
			if(entryNumber == null) {
				throw new IllegalStateException("NAK entry full");
			}
			nak.entryNumber = entryNumber;
			nak.offset = offset;
			nak.msgId = msgId; 
			
			writeNakUnsafe(nak);
		} finally {
			lock.unlock();
		}
	}
	
	public void removeNak(long offset) { 
		NakRecord nak = null;
		Iterator<NakRecord> iter = iterator();
		while(iter.hasNext()) {
			NakRecord t = iter.next();
			if(t.offset == offset) { 
				nak = t;
				break;
			}
		}
		if(nak == null) return;
		
		try {
			lock.lock(); 
			iter.remove();
			nak.status = 0;
			availableEntries.add(nak.entryNumber);
			writeNakUnsafe(nak);
		} finally {
			lock.unlock();
		}
	}

	@Override
	protected void loadDefaultData() throws IOException {  
		buffer.position(NakLimitPos);
		this.nakLimit = buffer.getInt();
		
		for(int i=0; i< NakMaxCount; i++) {
			NakRecord nak = readNakUnsafe(i);
			if(nak.status != 1) {
				availableEntries.add(i);
				continue;
			}
			nakQueue.add(nak); 
		}
	}

	@Override
	protected void writeDefaultData() throws IOException { 
		buffer.position(NakLimitPos);
		buffer.putInt(this.nakLimit);
	}  
	
	public int getNakLimit() {
		return this.nakLimit;
	}
	
	public void setNakLimit(int nakLimit) {
		if(nakLimit < 0 || nakLimit>=NakMaxCount) {
			throw new IllegalArgumentException("nakLimit(" + nakLimit + ") invalid");
		}
		this.nakLimit = nakLimit;
		try {
			lock.lock(); 
			buffer.position(NakLimitPos);
			buffer.putInt(this.nakLimit);
		} finally {
			lock.unlock();
		}
	}
	
	private int nakPosition(int entryNumber) {
		return HeadSize + entryNumber*NakRecord.Size;
	}
	
	private void writeNakUnsafe(NakRecord nak) {
		int pos = nakPosition(nak.entryNumber);
		buffer.position(pos); 
		buffer.put((byte)1);
		buffer.putLong(nak.offset);
		if(nak.msgId == null) {
			buffer.put((byte)0);
		} else {
			buffer.put((byte)nak.msgId.length());
			buffer.put(nak.msgId.getBytes());
		}
		buffer.position(pos + NakRecord.RetryCountPos);
		buffer.putInt(nak.retryCount);
		buffer.putLong(nak.updatedTime); 
	} 
	
	private NakRecord readNakUnsafe(int entryNumber) throws IOException { 
		NakRecord nak = new NakRecord();
		nak.entryNumber = entryNumber;
	
		int pos = nakPosition(entryNumber);
		buffer.position(pos); 
		
		nak.status = buffer.get();
		nak.offset = buffer.getLong();
		int len = buffer.get();
		if(len>0) {
			byte[] msgId = new byte[len];
			buffer.get(msgId);
			nak.msgId = new String(msgId);
		}
		buffer.position(pos + NakRecord.RetryCountPos);
		nak.retryCount = buffer.getInt();
		nak.updatedTime = buffer.getLong(); 
		
		return nak;
	}

	public NakRecord readNak(int entryNumber) throws IOException { 
		try {
			lock.lock();
			return readNakUnsafe(entryNumber);
		} finally {
			lock.unlock();
		}
	}
	
	public static class NakRecord {
		public byte status;     //1   0 - invalid, 1 - valid
		public long offset;	    //8
		public String msgId;    //40, max 39
		public int retryCount;  
		public long updatedTime = System.currentTimeMillis(); 
		
		public int entryNumber; //entry number, offset from 0, not persisted
		
		
		public final static int MsgIdMaxLen = 39;
		public final static int RetryCountPos = 1 + 8 + 1 + MsgIdMaxLen;
		public final static int Size = 1 + 8 + 1 + MsgIdMaxLen + 4 + 8; //61
	}
	
	public static class NakRecordComparator implements Comparator<NakRecord>{

		@Override
		public int compare(NakRecord o1, NakRecord o2) {  
			if(o1.updatedTime > o2.updatedTime) return 1;
			if(o1.updatedTime == o2.updatedTime) return 0;
			return -1;
		}
		
	}
}
