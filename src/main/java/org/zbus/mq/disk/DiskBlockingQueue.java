package org.zbus.mq.disk;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DiskBlockingQueue extends AbstractQueue<byte[]> implements BlockingQueue<byte[]> {
	private final DiskQueue support;
	private final String name;
	/** Main lock guarding all access */
	final ReentrantLock lock;
	/** Condition for waiting takes */
	private final Condition notEmpty;
	/** Condition for waiting puts */
	private final Condition notFull;

	public DiskBlockingQueue(DiskQueue support) {
		this.name = support.getQueueName();
		this.support = support;
		lock = new ReentrantLock();
		notEmpty = lock.newCondition();
		notFull = lock.newCondition();
	}

	@Override
	public boolean offer(byte[] e) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			boolean res = support.offer(e);
			notEmpty.signal();
			return res;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public byte[] poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			byte[] res = support.poll();
			notFull.signal();
			return res;
		} finally {
			lock.unlock();
		}
	}

	public byte[] take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while (support.size() == 0) {
				notEmpty.await();
			}
			return poll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int size() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return support.size();
		} finally {
			lock.unlock();
		}
	}

	public String getName() {
		return name;
	}

	@Override
	public void put(byte[] e) throws InterruptedException {
		offer(e);
	}

	@Override
	public boolean offer(byte[] e, long timeout, TimeUnit unit) throws InterruptedException {
		offer(e);
		return true;
	}

	@Override
	public byte[] poll(long timeout, TimeUnit unit) throws InterruptedException {
		return poll();
	}

	@Override
	public int remainingCapacity() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int drainTo(Collection<? super byte[]> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int drainTo(Collection<? super byte[]> c, int maxElements) {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] peek() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<byte[]> iterator() {
		throw new UnsupportedOperationException();
	}

}