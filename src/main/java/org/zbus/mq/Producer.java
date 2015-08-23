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
package org.zbus.mq;

import java.io.IOException;

import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;

public class Producer extends MqAdmin{  
	
	public Producer(Broker broker, String mq, MqMode... mode) {
		super(broker, mq, mode);
	} 
	
	public Producer(MqConfig config){
		super(config);
	}
	
	public void sendAsync(Message msg, final ResultCallback<Message> callback)
			throws IOException {
		msg.setCmd(Protocol.Produce);
		msg.setMq(this.mq); 
		msg.setAck(false);
		
		broker.invokeAsync(msg, callback);
	}
	
	public void sendAsync(Message msg) throws IOException {
		sendAsync(msg, null);
	}
	
	
	public Message sendSync(Message msg, int timeout) throws IOException, InterruptedException{
		msg.setCmd(Protocol.Produce);
		msg.setMq(this.mq); 
		msg.setAck(true);
		
		return broker.invokeSync(msg, timeout);
	}
	
	public Message sendSync(Message msg) throws IOException, InterruptedException{
		return sendSync(msg, 10000);
	}
	
}
