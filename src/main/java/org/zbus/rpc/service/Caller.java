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
package org.zbus.rpc.service;

import java.io.IOException;

import org.zbus.mq.Broker;
import org.zbus.mq.MqAdmin;
import org.zbus.mq.MqConfig;
import org.zbus.mq.Protocol;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;


public class Caller extends MqAdmin{    
	public Caller(Broker broker, String mq, MqMode... mode) {
		super(broker, mq, mode);
	}
	
	public Caller(MqConfig config){
		super(config);
	}
	
	private void fillCallMessage(Message req){
		req.setCmd(Protocol.Produce); 
		req.setMq(this.mq); 
		req.setAck(false);
	}

	
	public Message invokeSync(Message req, int timeout) throws IOException{ 
		this.fillCallMessage(req); 
		return broker.invokeSync(req, timeout);
	}
	
	public void invokeAsync(Message req, ResultCallback<Message> callback) throws IOException{
		this.fillCallMessage(req);   
		broker.invokeAsync(req, callback);
	}
}
