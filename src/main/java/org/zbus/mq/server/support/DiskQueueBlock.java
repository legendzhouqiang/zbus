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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.zbus.kit.log.Logger;

public class DiskQueueBlock implements Closeable{
	private static final Logger log = Logger.getLogger(DiskQueueBlock.class);
    private static final String BLOCK_FILE_SUFFIX = ".blk"; // 数据文件
    private static final int BLOCK_SIZE = 32 * 1024 * 1024; // 32MB

    private final int EOF = -1;

    private String blockFilePath;
    private DiskQueueIndex index;
    private RandomAccessFile blockFile;
    private FileChannel fileChannel; 
    private MappedByteBuffer byteBuffer;

    public DiskQueueBlock(String blockFilePath, DiskQueueIndex index, 
    		RandomAccessFile blockFile, FileChannel fileChannel,
            MappedByteBuffer byteBuffer) {
        this.blockFilePath = blockFilePath;
        this.index = index;
        this.blockFile = blockFile;
        this.fileChannel = fileChannel;
        this.byteBuffer = byteBuffer; 
    }

    public DiskQueueBlock(DiskQueueIndex index, String blockFilePath) {
        this.index = index;
        this.blockFilePath = blockFilePath;
        try {
            File file = new File(blockFilePath);
            this.blockFile = new RandomAccessFile(file, "rw");
            this.fileChannel = blockFile.getChannel();
            this.byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, BLOCK_SIZE);
            this.byteBuffer = byteBuffer.load();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public DiskQueueBlock duplicate() {
        return new DiskQueueBlock(this.blockFilePath, this.index, this.blockFile, this.fileChannel,
                (MappedByteBuffer)this.byteBuffer.duplicate());
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
        if (byteBuffer != null) {
            byteBuffer.force();
        }
    }

    public void close() {
        try {
            if (byteBuffer == null) {
                return;
            }
            sync();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @SuppressWarnings("restriction")
				public Object run() {
                    try {
                        Method getCleanerMethod = byteBuffer.getClass().getMethod("cleaner");
                        getCleanerMethod.setAccessible(true);
                        sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(byteBuffer);
                        cleaner.clean();
                    } catch (Exception e) {
                        log.error("close fqueue block file failed", e);
                    }
                    return null;
                }
            }); 
            byteBuffer = null;
            fileChannel.close();
            blockFile.close();
        } catch (IOException e) {
            log.error("close fqueue block file failed", e);
        }
    }
}