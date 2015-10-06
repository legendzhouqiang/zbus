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
package org.zbus.diskq;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class DiskQueue extends AbstractQueue<byte[]> {

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
        this.index = new DiskQueueIndex(queueName, fileBackupDir);
        this.size = new AtomicInteger(index.getWriteCounter() - index.getReadCounter());
        this.writeBlock = new DiskQueueBlock(index, queueName, index.getWriteNum(), fileBackupDir);
        if (index.getReadNum() == index.getWriteNum()) {
            this.readBlock = this.writeBlock.duplicate();
        } else {
            this.readBlock = new DiskQueueBlock(index, queueName, index.getReadNum(), fileBackupDir);
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
        writeBlock = new DiskQueueBlock(index, queueName, nextWriteBlockNum, fileBackupDir);
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
            readBlock = new DiskQueueBlock(index, queueName, nextReadBlockNum, fileBackupDir);
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