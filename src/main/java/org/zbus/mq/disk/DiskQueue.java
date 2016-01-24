package org.zbus.mq.disk;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class DiskQueue extends AbstractQueue<byte[]> {
    private String queueName;
    private String fileBackupPath;
    private DiskQueueIndex index;
    private DiskQueueBlock readBlock;
    private DiskQueueBlock writeBlock;
    private ReentrantLock readLock;
    private ReentrantLock writeLock;
    private AtomicInteger size;

    private final DiskQueuePool pool;
    protected DiskQueue(String queueName, DiskQueuePool pool) {
        this.queueName = queueName;
        this.pool = pool;
        this.fileBackupPath = this.pool.getFileBackupPath();
        this.readLock = new ReentrantLock();
        this.writeLock = new ReentrantLock();
        this.index = new DiskQueueIndex(DiskQueueIndex.formatIndexFilePath(queueName, fileBackupPath));
        this.size = new AtomicInteger(index.getWriteCounter() - index.getReadCounter());
        this.writeBlock = new DiskQueueBlock(index, DiskQueueBlock.formatBlockFilePath(queueName,
                index.getWriteNum(), fileBackupPath));
        if (index.getReadNum() == index.getWriteNum()) {
            this.readBlock = this.writeBlock.duplicate();
        } else {
            this.readBlock = new DiskQueueBlock(index, DiskQueueBlock.formatBlockFilePath(queueName,
                    index.getReadNum(), fileBackupPath));
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
                nextWriteBlockNum, fileBackupPath));
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
                    nextReadBlockNum, fileBackupPath));
        }
        index.putReadNum(nextReadBlockNum);
        index.putReadPosition(0);
        this.pool.toClear(blockPath);
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

	public String getQueueName() {
		return queueName;
	}
    
    
}