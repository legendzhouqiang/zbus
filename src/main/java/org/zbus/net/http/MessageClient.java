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
package org.zbus.net.http;

import java.io.IOException;

import org.zbus.net.InvokingClient;
import org.zbus.net.core.Dispatcher;


public class MessageClient extends InvokingClient<Message, Message>{   
	public MessageClient(String host, int port, Dispatcher dispatcher){
		super(host, port, dispatcher);
		codec(new MessageCodec());
	} 
	
	public MessageClient(String address, Dispatcher dispatcher) {
		super(address, dispatcher);
		codec(new MessageCodec());
	}
	
	@Override
	protected void heartbeat() {
		if(this.hasConnected()){
			Message hbt = new Message();
			hbt.setCmd(Message.HEARTBEAT);
			try {
				this.send(hbt);
			} catch (IOException e) {  
				//ignore
			}
		}
	}
}
