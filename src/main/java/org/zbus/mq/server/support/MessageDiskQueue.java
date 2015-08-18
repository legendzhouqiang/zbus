package org.zbus.mq.server.support;

import java.util.AbstractQueue;
import java.util.Iterator;

import org.zbus.mq.server.support.DiskQueuePool.DiskQueue;
import org.zbus.net.core.IoBuffer;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageCodec;

public class MessageDiskQueue extends AbstractQueue<Message>{
	private static final MessageCodec codec = new MessageCodec();
	
	private final DiskQueue diskQueue;
	private final String name;
	
	public MessageDiskQueue(String name, DiskQueue diskQueue){
		this.name = name;
		this.diskQueue = diskQueue;
	}
	
	public MessageDiskQueue(String name, int flag){
		this.name = name; 
		this.diskQueue = DiskQueuePool.getDiskQueue(name);
		this.diskQueue.setFlag(flag);
	}
	public MessageDiskQueue(String name){
		this(name, 0);
	}
	
	public static void init(String deployPath) {
		DiskQueuePool.init(deployPath);
    }
	
	public String getName() {
		return name;
	}

	@Override
	public boolean offer(Message msg) {  
		return diskQueue.offer(msg.toBytes());
	}

	@Override
	public Message poll() {
		byte[] bytes = diskQueue.poll();
		if(bytes == null) return null;
		return (Message)codec.decode(IoBuffer.wrap(bytes));
	}

	@Override
	public Message peek() { 
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Message> iterator() { 
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() { 
		return diskQueue.size();
	}
}