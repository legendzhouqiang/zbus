package org.zbus.mq.disk;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Index extends MappedFile {
	public static final String IndexSuffix = ".idx";
	public static final String BlockSuffix = ".zbus";
	public static final String BlockDir = "data";
	public static final String ReaderDir = "reader";

	public static final int BlockMaxCount = 10240;
	public static final long BlockMaxSize = 64 * 1024 * 1024; // default to 64M

	public static final int OffsetSize = 20;
	public static final int IndexHeadSize = 1024;
	public static final int IndexSize = IndexHeadSize + BlockMaxCount * OffsetSize;

	// Extension
	private static final int ExtItemSize = 128;
	private static final int ExtItemCount = 4;
	private static final int ExtOffset = IndexHeadSize - ExtItemSize * ExtItemCount;

	private volatile int blockCount = 0;
	private volatile int flag = 0;
	public final AtomicReference<CountDownLatch> newDataAvailable = new AtomicReference<CountDownLatch>(
			new CountDownLatch(1));;

	private File indexDir;
	private Lock lock = new ReentrantLock();
	private final String name;

	private String[] extentions = new String[ExtItemCount];

	public Index(File dir) throws IOException {
		this.indexDir = dir;
		this.name = indexDir.getName();
		File file = new File(indexDir, this.indexDir.getName() + IndexSuffix);
		load(file, IndexSize);
	}

	/**
	 * Write endOffset of last block
	 * 
	 * @param endOffset
	 *            writable offset of last block
	 * @throws IOException
	 */
	public void writeEndOffset(int endOffset) throws IOException {
		try {
			lock.lock();
			buffer.position(IndexHeadSize + (blockCount - 1) * OffsetSize + 16);
			buffer.putInt(endOffset);
		} finally {
			lock.unlock();
		}
	}

	public int readEndOffset() throws IOException {
		try {
			lock.lock();
			buffer.position(IndexHeadSize + (blockCount - 1) * OffsetSize + 16);
			return buffer.getInt();
		} finally {
			lock.unlock();
		}
	}

	public Block createWriteBlock() throws IOException {
		Offset offset = null;
		int blockNumber;
		try {
			lock.lock();
			if (blockCount < 1 || isLastBlockFull()) {
				blockNumber = blockCount;
				offset = addNewOffset();
			} else {
				blockNumber = blockCount - 1;
				offset = readOffsetUnsafe(blockNumber);
			}
		} finally {
			lock.unlock();
		}

		Block block = new Block(this, blockFile(offset.baseOffset), blockNumber);
		return block;
	}

	public Block createReadBlock(int blockNumber) throws IOException {
		if (blockCount < 1) {
			throw new IllegalStateException("No block to read");
		}
		checkBlockNumber(blockNumber);

		Offset offset = readOffset(blockNumber);
		Block block = new Block(this, blockFile(offset.baseOffset), blockNumber);
		return block;
	}

	public Offset readOffset(int blockNumber) throws IOException {
		checkBlockNumber(blockNumber);
		try {
			lock.lock();
			return readOffsetUnsafe(blockNumber);
		} finally {
			lock.unlock();
		}
	}

	private Offset readOffsetUnsafe(int blockNumber) throws IOException {
		buffer.position(IndexHeadSize + blockNumber * OffsetSize);

		Offset offset = new Offset();
		offset.createdTime = buffer.getLong();
		offset.baseOffset = buffer.getLong();
		offset.endOffset = buffer.getInt();
		return offset;
	}

	private void checkBlockNumber(int blockNumber) {
		if (blockNumber < 0 || blockNumber >= blockCount) {
			throw new IllegalArgumentException(
					"blockNumber should >=0 and <" + blockCount + ", but was " + blockNumber);
		}
	}

	/**
	 * Search block number by totalOffset
	 * 
	 * @param readOffset
	 *            offset from block 0, not the offset in the block.
	 * @return block number the tottalOffset follows.
	 * @throws IOException
	 */
	public int searchBlockNumber(long totalOffset) throws IOException {
		for (int i = 0; i < blockCount; i++) {
			Offset offset = readOffset(i);
			if (totalOffset >= offset.baseOffset && totalOffset < offset.baseOffset + offset.endOffset) {
				return i;
			}
		}
		return -1;
	}

	public File getIndexDir() {
		return indexDir;
	}

	public int getBlockCount() {
		return blockCount;
	}

	private File blockFile(long baseOffset) {
		String fileName = String.format("%020d%s", baseOffset, BlockSuffix);
		File blockDir = new File(indexDir, BlockDir);
		return new File(blockDir, fileName);
	}

	private void writeOffset(int blockNumber, Offset offset) {
		buffer.position(IndexHeadSize + blockNumber * OffsetSize);

		buffer.putLong(offset.createdTime);
		buffer.putLong(offset.baseOffset);
		buffer.putInt(offset.endOffset);
	}

	private Offset addNewOffset() throws IOException {
		if (blockCount >= BlockMaxCount) {
			throw new IllegalStateException("Offset table full");
		}

		long baseOffset = 0;
		if (blockCount > 0) {
			Offset offset = readOffsetUnsafe(blockCount - 1);
			baseOffset = offset.baseOffset + offset.endOffset;
		}

		Offset offset = new Offset();
		offset.createdTime = System.currentTimeMillis();
		offset.baseOffset = baseOffset;
		offset.endOffset = 0;

		writeOffset(blockCount, offset);

		blockCount++;
		writeBlockCount();

		return offset;
	}

	private boolean isLastBlockFull() {
		if (blockCount < 1)
			return false;

		buffer.position(IndexHeadSize + (blockCount - 1) * OffsetSize + 16);
		int endOffset = buffer.getInt();
		return endOffset >= BlockMaxSize;
	}

	private void writeBlockCount() {
		buffer.position(0);
		buffer.putInt(blockCount);
	}

	@Override
	protected void loadDefaultData() throws IOException {
		buffer.position(0);
		this.blockCount = buffer.getInt();
		this.flag = buffer.getInt();
		readExt();
	}

	@Override
	protected void writeDefaultData() throws IOException {
		writeBlockCount();
		buffer.putInt(this.flag);
		initExt();
	}

	private void initExt() {
		for (int i = 0; i < ExtItemCount; i++) {
			setExt(i, null);
		}
	}

	private void readExt() throws IOException {
		for (int i = 0; i < ExtItemCount; i++) {
			readExtByIndex(i);
		}
	}

	private void readExtByIndex(int idx) throws IOException {
		this.buffer.position(ExtOffset + ExtItemSize * idx);
		int len = buffer.get();
		if (len <= 0) {
			this.extentions[idx] = null;
			return;
		}
		if (len > ExtItemSize - 1) {
			throw new IOException("length of extension field invalid, too long");
		}
		byte[] bb = new byte[len];
		this.buffer.get(bb);
		this.extentions[idx] = new String(bb);
	}

	public void setExt(int idx, String value) {
		if (idx < 0) {
			throw new IllegalArgumentException("idx must >=0");
		}
		if (idx >= ExtItemCount) {
			throw new IllegalArgumentException("idx must <" + ExtItemCount);
		}
		try {
			lock.lock();
			this.extentions[idx] = value;
			this.buffer.position(ExtOffset + ExtItemSize * idx);
			if (value == null) {
				this.buffer.put((byte) 0);
				return;
			}
			if (value.length() > ExtItemSize - 1) {
				throw new IllegalArgumentException(value + " too long");
			}
			this.buffer.put((byte) value.length());
			this.buffer.put(value.getBytes());
		} finally {
			lock.unlock();
		}
	}

	public String getExt(int idx) {
		if (idx < 0) {
			throw new IllegalArgumentException("idx must >=0");
		}
		if (idx >= ExtItemCount) {
			throw new IllegalArgumentException("idx must <" + ExtItemCount);
		}
		return this.extentions[idx];
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(int flag) {
		this.flag = flag;
		try {
			lock.lock();
			this.buffer.position(4);
			this.buffer.putInt(this.flag);
		} finally {
			lock.unlock();
		}
	}
	
	

	public String getName() {
		return name;
	}



	public static class Offset {
		public long baseOffset;
		public long createdTime;
		public int endOffset;
	}
}
