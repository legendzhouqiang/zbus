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
import java.util.concurrent.LinkedBlockingQueue;

import org.zbus.net.http.Message;

public class MessageMemoryQueue extends MessageQueue { 
	private LinkedBlockingQueue<Message> support = new LinkedBlockingQueue<Message>();
	private String slaveToMq;
	private String accessToken;
	private String creator;
	
	@Override
	public boolean offer(Message e) {
		return support.offer(e);
	}

	@Override
	public Message poll() {
		return support.poll();
	}

	@Override
	public Message peek() { 
		return support.peek();
	}

	@Override
	public String getMasterMq() {
		return this.slaveToMq;
	}

	@Override
	public void setMasterMq(String value) {
		this.slaveToMq = value;
	}

	@Override
	public Iterator<Message> iterator() {
		return support.iterator();
	}

	@Override
	public int size() {
		return support.size();
	}

	@Override
	public String getAccessToken() { 
		return this.accessToken;
	}

	@Override
	public void setAccessToken(String value) {
		this.accessToken = value;
	}

	@Override
	public String getCreator() {
		return this.creator;
	}

	@Override
	public void setCreator(String value) {
		this.creator = value;
	}
}
