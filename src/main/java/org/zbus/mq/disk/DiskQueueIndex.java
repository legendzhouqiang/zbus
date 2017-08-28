package org.zbus.mq.disk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;

public class DiskQueueIndex {
	static final Logger log = LoggerFactory.getLogger(DiskQueueIndex.class);
    private static final String MAGIC = "v100";
    private static final String INDEX_FILE_SUFFIX = ".idx";
    private static final int EXT_ITEM_SIZE = 256;
    private static final int EXT_ITEM_CNT = 4;
    private static final int INDEX_SIZE = 32 + EXT_ITEM_SIZE*EXT_ITEM_CNT;
    
    private static final int FLAG_OFFSET = 4;
    private static final int READ_NUM_OFFSET = 8;
    private static final int READ_POS_OFFSET = 12;
    private static final int READ_CNT_OFFSET = 16;
    private static final int WRITE_NUM_OFFSET = 20;
    private static final int WRITE_POS_OFFSET = 24;
    private static final int WRITE_CNT_OFFSET = 28;
    
    private static final int EXT_OFFSET = 32;
    private String[] extentions = new String[EXT_ITEM_CNT];
    

    private int flag;                    // 4
    private volatile int readNum;        // 8  
    private volatile int readPosition;   // 12   
    private volatile int readCounter;    // 16    
    private volatile int writeNum;       // 20  
    private volatile int writePosition;  // 24    
    private volatile int writeCounter;   // 28  
 
    

    private RandomAccessFile indexFile;
    private FileChannel fileChannel;
    // 读写分离
    private MappedByteBuffer writeIndex;
    private MappedByteBuffer readIndex;
     
    public DiskQueueIndex(String indexFilePath) { 
        File file = new File(indexFilePath);
        try {
            if (file.exists()) {
                this.indexFile = new RandomAccessFile(file, "rw");
                byte[] bytes = new byte[4];
                this.indexFile.read(bytes, 0, 4);
                if (!MAGIC.equals(new String(bytes))) {
                    throw new IllegalArgumentException("version mismatch");
                }
                long size = indexFile.length();
                if(size < INDEX_SIZE){ 
                	indexFile.setLength(INDEX_SIZE); 
                	indexFile.seek(size);
                	indexFile.write(new byte[(int)(INDEX_SIZE-size)]); 
                }
                
                indexFile.seek(4);
                this.flag = indexFile.readInt();
                this.readNum = indexFile.readInt();
                this.readPosition = indexFile.readInt();
                this.readCounter = indexFile.readInt();
                this.writeNum = indexFile.readInt();
                this.writePosition = indexFile.readInt();
                this.writeCounter = indexFile.readInt();
                
                this.readExt();
                
                this.fileChannel = indexFile.getChannel();
                this.writeIndex = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, INDEX_SIZE);
                this.writeIndex = writeIndex.load();
                this.readIndex = (MappedByteBuffer) writeIndex.duplicate();
            } else {
                this.indexFile = new RandomAccessFile(file, "rw");
                this.fileChannel = indexFile.getChannel();
                this.writeIndex = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, INDEX_SIZE);
                this.readIndex = (MappedByteBuffer) writeIndex.duplicate();
                putMagic();
                putFlag(0);
                putReadNum(0);
                putReadPosition(0);
                putReadCounter(0);
                putWriteNum(0);
                putWritePosition(0);
                putWriteCounter(0);
                
                this.initExt();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static boolean isIndexFile(String fileName) {
        return fileName.endsWith(INDEX_FILE_SUFFIX);
    }

    public static String parseQueueName(String indexFileName) {
        String fileName = indexFileName.substring(0, indexFileName.lastIndexOf('.'));
        return fileName;
    }

    public static String formatIndexFilePath(String queueName, String fileBackupDir) {
        return fileBackupDir + File.separator + String.format("%s%s", queueName, INDEX_FILE_SUFFIX);
    }
    
    public static String indexFileName(String queueName){
    	return String.format("%s%s", queueName, INDEX_FILE_SUFFIX);
    }
    
    public int getFlag(){
    	return this.flag;
    }

    public int getReadNum() {
        return this.readNum;
    }

    public int getReadPosition() {
        return this.readPosition;
    }

    public int getReadCounter() {
        return this.readCounter;
    }

    public int getWriteNum() {
        return this.writeNum;
    }

    public int getWritePosition() {
        return this.writePosition;
    }

    public int getWriteCounter() {
        return this.writeCounter;
    }

    public void putMagic() {
        this.writeIndex.position(0);
        this.writeIndex.put(MAGIC.getBytes());
    }
    
    public void putFlag(int flag){
    	this.writeIndex.position(FLAG_OFFSET);
    	this.writeIndex.putInt(flag);
    	this.flag = flag;
    }

    public void putWritePosition(int writePosition) {
        this.writeIndex.position(WRITE_POS_OFFSET);
        this.writeIndex.putInt(writePosition);
        this.writePosition = writePosition;
    }

    public void putWriteNum(int writeNum) {
        this.writeIndex.position(WRITE_NUM_OFFSET);
        this.writeIndex.putInt(writeNum);
        this.writeNum = writeNum;
    }

    public void putWriteCounter(int writeCounter) {
        this.writeIndex.position(WRITE_CNT_OFFSET);
        this.writeIndex.putInt(writeCounter);
        this.writeCounter = writeCounter;
    }

    public void putReadNum(int readNum) {
        this.readIndex.position(READ_NUM_OFFSET);
        this.readIndex.putInt(readNum);
        this.readNum = readNum;
    }

    public void putReadPosition(int readPosition) {
        this.readIndex.position(READ_POS_OFFSET);
        this.readIndex.putInt(readPosition);
        this.readPosition = readPosition;
    }

    public void putReadCounter(int readCounter) {
        this.readIndex.position(READ_CNT_OFFSET);
        this.readIndex.putInt(readCounter);
        this.readCounter = readCounter;
    }
    
    private void initExt(){
    	for(int i=0; i<EXT_ITEM_CNT; i++){
    		putExt(i, null);
    	}
    }
    private void readExt() throws IOException{
    	for(int i=0; i<EXT_ITEM_CNT; i++){
    		readExtByIndex(i);
    	}
    }
    
    private void readExtByIndex(int idx) throws IOException{
    	if(idx < 0 || idx >= EXT_ITEM_CNT){
    		throw new IllegalArgumentException("idx invalid");
    	}
    	this.indexFile.seek(EXT_OFFSET + EXT_ITEM_SIZE*idx);
    	int len = indexFile.readByte();
    	if(len <= 0){
    		this.extentions[idx] = null;
    		return;
    	}
    	if(len > EXT_ITEM_SIZE -1){
    		throw new IOException("lenght of ext1 invalid, too long");
    	}
    	byte[] bb = new byte[len];
    	this.indexFile.read(bb, 0, len); 
    	this.extentions[idx] = new String(bb);
    }
    
    
    public void putExt(int idx, String value){
    	if(idx < 0 || idx >= EXT_ITEM_CNT){
    		throw new IllegalArgumentException("idx invalid");
    	}
    	this.extentions[idx] = value;
    	this.readIndex.position(EXT_OFFSET + EXT_ITEM_SIZE*idx);
    	if(value == null){
    		this.readIndex.put((byte)0);
    		return;
    	}
    	if(value.length() > EXT_ITEM_SIZE - 1){
    		throw new IllegalArgumentException(value + " too long");
    	} 
    	this.readIndex.put((byte)value.length());
    	this.readIndex.put(value.getBytes());
    	
    }
    
    public String getExt(int idx){
    	if(idx < 0 || idx >= EXT_ITEM_CNT){
    		throw new IllegalArgumentException("idx invalid");
    	}
    	return this.extentions[idx];
    }

    public void reset() {
        int size = writeCounter - readCounter;
        putReadCounter(0);
        putWriteCounter(size);
        if (size == 0 && readNum == writeNum) {
            putReadPosition(0);
            putWritePosition(0);
        }
    }

    public void sync() {
        if (writeIndex != null) {
            writeIndex.force();
        }
    }

    public void close() {
        try {
            if (writeIndex == null) {
                return;
            }
            sync();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
            	@SuppressWarnings("restriction")
                public Object run() {
                    try {
                        Method getCleanerMethod = writeIndex.getClass().getMethod("cleaner");
                        getCleanerMethod.setAccessible(true); 
						sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(writeIndex);
                        cleaner.clean();
                    } catch (Exception e) {
                        log.error("close fqueue index file failed", e);
                    }
                    return null;
                }
            });
            writeIndex = null;
            readIndex = null;
            fileChannel.close();
            indexFile.close();
        } catch (IOException e) {
            log.error("close fqueue index file failed", e);
        }
    }
}