/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.mq.server.support;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.AbstractQueue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.zbus.log.Logger;

/**
 *  Many thanks to @huamingweiwen in OSChina who presented us a light weight solution
 *  of disk-based queue design and implementation, DiskQueuePool is mainly refined from
 *  his brilliant work.
 *  
 *  http://my.oschina.net/xnkl/blog/477690
 */
public class DiskQueuePool { 
    private static final Logger log = Logger.getLogger(DiskQueuePool.class);
    private static final BlockingQueue<String> deletingQueue = new LinkedBlockingQueue<String>();
    
    private static DiskQueuePool instance = null;
    private String fileBackupPath;
    private Map<String, DiskQueue> queueMap;
    private ScheduledExecutorService syncService;
 
    private DiskQueuePool(String fileBackupPath) {
        this.fileBackupPath = fileBackupPath;
        File fileBackupDir = new File(fileBackupPath);
        if (!fileBackupDir.exists() && !fileBackupDir.mkdir()) {
            throw new IllegalArgumentException("can not create directory");
        }
        this.queueMap = scanDir(fileBackupDir);
        this.syncService = Executors.newSingleThreadScheduledExecutor();
        this.syncService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (DiskQueue q : queueMap.values()) {
                    q.sync();
                }
                deleteBlockFile();
            }
        }, 1000, 10000, TimeUnit.MILLISECONDS);
    }
    
    public static Map<String, DiskQueue> getQueryMap(){
    	return instance.queueMap;
    }
 
    private void deleteBlockFile() {
        String blockFilePath = null;
        while( (blockFilePath = deletingQueue.poll()) != null){
        	blockFilePath = blockFilePath.trim();
        	if(blockFilePath.equals("")) continue;
        	log.info("Delete File[%s]", blockFilePath);   
            File delFile = new File(blockFilePath);
            try {
                if (!delFile.delete()) {
                    log.warn("block file:%s delete failed", blockFilePath);
                }
            } catch (SecurityException e) {
                log.error("security manager exists, delete denied");
            } 
        }
    }
 
    static void toClear(String filePath) {
        deletingQueue.add(filePath);
    }
 
    private Map<String, DiskQueue> scanDir(File fileBackupDir) {
        if (!fileBackupDir.isDirectory()) {
            throw new IllegalArgumentException("it is not a directory");
        }
        Map<String, DiskQueue> queues = new HashMap<String, DiskQueue>();
        File[] indexFiles = fileBackupDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return DiskQueueIndex.isIndexFile(name);
            }
        });
        if (indexFiles != null && indexFiles.length> 0) {
            for (File indexFile : indexFiles) {
                String queueName = DiskQueueIndex.parseQueueName(indexFile.getName());
                queues.put(queueName, new DiskQueue(queueName, fileBackupPath));
            }
        }
        return queues;
    }
 
    public synchronized static void init(String deployPath) {
        if (instance == null) {
            instance = new DiskQueuePool(deployPath);
        }
    }
 
    private void dispose() {
        this.syncService.shutdown();
        for (DiskQueue q : queueMap.values()) {
            q.close();
        }
        while (!deletingQueue.isEmpty()) {
            deleteBlockFile();
        }
    }
 
    public synchronized static void destory() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }
 
    private DiskQueue getQueueFromPool(String queueName) {
        if (queueMap.containsKey(queueName)) {
            return queueMap.get(queueName);
        }
        DiskQueue q = new DiskQueue(queueName, fileBackupPath);
        queueMap.put(queueName, q);
        return q;
    }
 
    public synchronized static DiskQueue getDiskQueue(String queueName) {
    	if(instance == null){
    		throw new IllegalStateException("call DiskQueuePool.init(dir) first");
    	}
        if (queueName==null || queueName.trim().equals("")) {
            throw new IllegalArgumentException("empty queue name");
        }
        return instance.getQueueFromPool(queueName);
    }
 
    public static class DiskQueue extends AbstractQueue<byte[]> {

        private String queueName;
        private String fileBackupDir;
        private DiskQueueIndex index;
        private DiskQueueBlock readBlock;
        private DiskQueueBlock writeBlock;
        private ReentrantLock readLock;
        private ReentrantLock writeLock;
        private AtomicInteger size;

        public DiskQueue(String queueName, String fileBackupDir) {
            this.queueName = queueName;
            this.fileBackupDir = fileBackupDir;
            this.readLock = new ReentrantLock();
            this.writeLock = new ReentrantLock();
            this.index = new DiskQueueIndex(DiskQueueIndex.formatIndexFilePath(queueName, fileBackupDir));
            this.size = new AtomicInteger(index.getWriteCounter() - index.getReadCounter());
            this.writeBlock = new DiskQueueBlock(index, DiskQueueBlock.formatBlockFilePath(queueName,
                    index.getWriteNum(), fileBackupDir));
            if (index.getReadNum() == index.getWriteNum()) {
                this.readBlock = this.writeBlock.duplicate();
            } else {
                this.readBlock = new DiskQueueBlock(index, DiskQueueBlock.formatBlockFilePath(queueName,
                        index.getReadNum(), fileBackupDir));
            }
        }
        
        public int getFlag(){
        	return index.getFlag();
        }
        
        public void setFlag(int flag){
        	index.putFlag(flag);
        }

        @Override
        public Iterator<byte[]> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return this.size.get();
        }

        private void rotateNextWriteBlock() {
            int nextWriteBlockNum = index.getWriteNum() + 1;
            nextWriteBlockNum = (nextWriteBlockNum < 0) ? 0 : nextWriteBlockNum;
            writeBlock.putEOF();
            if (index.getReadNum() == index.getWriteNum()) {
                writeBlock.sync();
            } else {
                writeBlock.close();
            }
            writeBlock = new DiskQueueBlock(index, DiskQueueBlock.formatBlockFilePath(queueName,
                    nextWriteBlockNum, fileBackupDir));
            index.putWriteNum(nextWriteBlockNum);
            index.putWritePosition(0);
        }

        @Override
        public boolean offer(byte[] bytes) {
            if (bytes == null || bytes.length == 0) {
                return true;
            }
            writeLock.lock();
            try {
                if (!writeBlock.isSpaceAvailable(bytes.length)) {
                    rotateNextWriteBlock();
                }
                writeBlock.write(bytes);
                size.incrementAndGet();
                return true;
            } finally {
                writeLock.unlock();
            }
        }

        private void rotateNextReadBlock() {
            if (index.getReadNum() == index.getWriteNum()) {
                // 读缓存块的滑动必须发生在写缓存块滑动之后
                return;
            }
            int nextReadBlockNum = index.getReadNum() + 1;
            nextReadBlockNum = (nextReadBlockNum < 0) ? 0 : nextReadBlockNum;
            readBlock.close();
            String blockPath = readBlock.getBlockFilePath();
            if (nextReadBlockNum == index.getWriteNum()) {
                readBlock = writeBlock.duplicate();
            } else {
                readBlock = new DiskQueueBlock(index, DiskQueueBlock.formatBlockFilePath(queueName,
                        nextReadBlockNum, fileBackupDir));
            }
            index.putReadNum(nextReadBlockNum);
            index.putReadPosition(0);
            DiskQueuePool.toClear(blockPath);
        }

        @Override
        public byte[] poll() {
            readLock.lock();
            try {
                if (readBlock.eof()) {
                    rotateNextReadBlock();
                }
                byte[] bytes = readBlock.read();
                if (bytes != null) {
                    size.decrementAndGet();
                }
                return bytes;
            } finally {
                readLock.unlock();
            }
        }

        @Override
        public byte[] peek() {
            throw new UnsupportedOperationException();
        }

        public void sync() {
            index.sync();
            // read block只读，不用同步
            writeBlock.sync();
        }

        public void close() {
            writeBlock.close();
            if (index.getReadNum() != index.getWriteNum()) {
                readBlock.close();
            }
            index.reset();
            index.close();
        }
    }
    static class DiskQueueIndex {
 
        private static final String MAGIC = "v100";
        private static final String INDEX_FILE_SUFFIX = ".idx";
        private static final int INDEX_SIZE = 32;
        
        private static final int FLAG_OFFSET = 4;
        private static final int READ_NUM_OFFSET = 8;
        private static final int READ_POS_OFFSET = 12;
        private static final int READ_CNT_OFFSET = 16;
        private static final int WRITE_NUM_OFFSET = 20;
        private static final int WRITE_POS_OFFSET = 24;
        private static final int WRITE_CNT_OFFSET = 28;
 
        private int flag;
        
        @SuppressWarnings("unused")
		private int p11, p12, p13, p14, p15, p16, p17, p18; // 缓存行填充 32B
        private volatile int readPosition;   // 12   读索引位置
        private volatile int readNum;        // 8   读索引文件号
        private volatile int readCounter;    // 16   总读取数量
        
        @SuppressWarnings("unused")
		private int p21, p22, p23, p24, p25, p26, p27, p28; // 缓存行填充 32B
        private volatile int writePosition;  // 24  写索引位置
        private volatile int writeNum;       // 20  写索引文件号
        private volatile int writeCounter;   // 28 总写入数量
    
        @SuppressWarnings("unused")
		private int p31, p32, p33, p34, p35, p36, p37, p38; // 缓存行填充 32B
 
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
                    this.flag = indexFile.readInt();
                    this.readNum = indexFile.readInt();
                    this.readPosition = indexFile.readInt();
                    this.readCounter = indexFile.readInt();
                    this.writeNum = indexFile.readInt();
                    this.writePosition = indexFile.readInt();
                    this.writeCounter = indexFile.readInt();
                    
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
 
    static class DiskQueueBlock {
 
        private static final String BLOCK_FILE_SUFFIX = ".blk"; // 数据文件
        private static final int BLOCK_SIZE = 32 * 1024 * 1024; // 32MB
 
        private final int EOF = -1;
 
        private String blockFilePath;
        private DiskQueueIndex index;
        private RandomAccessFile blockFile;
        private FileChannel fileChannel;
        private ByteBuffer byteBuffer;
        private MappedByteBuffer mappedBlock;
 
        public DiskQueueBlock(String blockFilePath, DiskQueueIndex index, RandomAccessFile blockFile, FileChannel fileChannel,
                            ByteBuffer byteBuffer, MappedByteBuffer mappedBlock) {
            this.blockFilePath = blockFilePath;
            this.index = index;
            this.blockFile = blockFile;
            this.fileChannel = fileChannel;
            this.byteBuffer = byteBuffer;
            this.mappedBlock = mappedBlock;
        }
 
        public DiskQueueBlock(DiskQueueIndex index, String blockFilePath) {
            this.index = index;
            this.blockFilePath = blockFilePath;
            try {
                File file = new File(blockFilePath);
                this.blockFile = new RandomAccessFile(file, "rw");
                this.fileChannel = blockFile.getChannel();
                this.mappedBlock = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, BLOCK_SIZE);
                this.byteBuffer = mappedBlock.load();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
 
        public DiskQueueBlock duplicate() {
            return new DiskQueueBlock(this.blockFilePath, this.index, this.blockFile, this.fileChannel,
                    this.byteBuffer.duplicate(), this.mappedBlock);
        }
 
        public static String formatBlockFilePath(String queueName, int fileNum, String fileBackupDir) {
            return fileBackupDir + File.separator + String.format("%s_%d%s", queueName, fileNum, BLOCK_FILE_SUFFIX);
        }
 
        public String getBlockFilePath() {
            return blockFilePath;
        }
 
        public void putEOF() {
            this.byteBuffer.position(index.getWritePosition());
            this.byteBuffer.putInt(EOF);
        }
 
        public boolean isSpaceAvailable(int len) {
            int increment = len + 4;
            int writePosition = index.getWritePosition();
            return BLOCK_SIZE >= increment + writePosition + 4; // 保证最后有4字节的空间可以写入EOF
        }
 
        public boolean eof() {
            int readPosition = index.getReadPosition();
            return readPosition > 0 && byteBuffer.getInt(readPosition) == EOF;
        }
 
        public int write(byte[] bytes) {
            int len = bytes.length;
            int increment = len + 4;
            int writePosition = index.getWritePosition();
            byteBuffer.position(writePosition);
            byteBuffer.putInt(len);
            byteBuffer.put(bytes);
            index.putWritePosition(increment + writePosition);
            index.putWriteCounter(index.getWriteCounter() + 1);
            return increment;
        }
        
        public int write(byte[] bytes, int start, int len) { 
            int increment = len + 4;
            int writePosition = index.getWritePosition();
            byteBuffer.position(writePosition);
            byteBuffer.putInt(len); 
            byteBuffer.put(bytes, start, len);
            index.putWritePosition(increment + writePosition);
            index.putWriteCounter(index.getWriteCounter() + 1);
            return increment;
        }
 
        public byte[] read() {
            byte[] bytes;
            int readNum = index.getReadNum();
            int readPosition = index.getReadPosition();
            int writeNum = index.getWriteNum();
            int writePosition = index.getWritePosition();
            if (readNum == writeNum && readPosition >= writePosition) {
                return null;
            }
            byteBuffer.position(readPosition);
            int dataLength = byteBuffer.getInt();
            if (dataLength <= 0) {
                return null;
            }
            bytes = new byte[dataLength];
            byteBuffer.get(bytes);
            index.putReadPosition(readPosition + bytes.length + 4);
            index.putReadCounter(index.getReadCounter() + 1);
            return bytes;
        }
 
        public void sync() {
            if (mappedBlock != null) {
                mappedBlock.force();
            }
        }
 
        public void close() {
            try {
                if (mappedBlock == null) {
                    return;
                }
                sync();
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @SuppressWarnings("restriction")
					public Object run() {
                        try {
                            Method getCleanerMethod = mappedBlock.getClass().getMethod("cleaner");
                            getCleanerMethod.setAccessible(true);
                            sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(mappedBlock);
                            cleaner.clean();
                        } catch (Exception e) {
                            log.error("close fqueue block file failed", e);
                        }
                        return null;
                    }
                });
                mappedBlock = null;
                byteBuffer = null;
                fileChannel.close();
                blockFile.close();
            } catch (IOException e) {
                log.error("close fqueue block file failed", e);
            }
        }
    }
 
}