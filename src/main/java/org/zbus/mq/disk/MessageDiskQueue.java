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
package org.zbus.mq.disk;

import java.util.Iterator;

import org.zbus.net.http.IoBuffer;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageCodec;

public class MessageDiskQueue extends MessageQueue{
	private static final MessageCodec codec = new MessageCodec();
	
	private final DiskQueue diskQueue;
	private final String name;
	
	public MessageDiskQueue(String name, int flag, DiskQueue diskQueue){
		this.name = name;
		this.diskQueue = diskQueue;
		this.diskQueue.setFlag(flag);
	}
	
	public MessageDiskQueue(String name, DiskQueue diskQueue){
		this(name, 0, diskQueue);
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
	
	@Override
	public String getMasterMq() { 
		return diskQueue.getExt(0);
	}

	@Override
	public void setMasterMq(String value) {
		diskQueue.setExt(0, value);
	}

	@Override
	public String getAccessToken() {
		return diskQueue.getExt(1);
	}

	@Override
	public void setAccessToken(String value) {
		diskQueue.setExt(1, value);
	}

	@Override
	public String getCreator() {
		return diskQueue.getExt(2);
	}

	@Override
	public void setCreator(String value) {
		diskQueue.setExt(2, value);
	}

	public DiskQueue getDiskQueue() {
		return diskQueue;
	} 
	
	
}