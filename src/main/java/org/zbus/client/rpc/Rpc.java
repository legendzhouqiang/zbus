/************************************************************************
 *  Copyright (c) 2011-2012 HONG LEIMING.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 ***************************************************************************/
package org.zbus.client.rpc;


import java.io.IOException;

import org.remoting.Message;
import org.remoting.RemotingClient;
import org.remoting.ticket.ResultCallback;
import org.zbus.client.ClientPool;
import org.zbus.client.InvokeHelper;
import org.zbus.common.Proto;

public class Rpc {  
	protected ClientPool pool; 
	protected RemotingClient client = null;
	protected String mq;
	protected String token = "";  
 
	public Rpc(ClientPool pool, String mq) {
		this.pool = pool;
		this.mq = mq;
	}
	 
	public Rpc(RemotingClient client, String mq) {
		this.client = client;
		this.mq = mq;
	}
	
	
	public Message invokeSync(Message req, int timeout) throws IOException{ 
		req.setCommand(Proto.Request); 
		req.setMq(this.mq);
		req.setToken(this.token);  
    	 
		return InvokeHelper.invokeSync(pool, client, req, timeout);
	}
	
	public void invokeAsync(Message req, ResultCallback callback) throws IOException{
		req.setCommand(Proto.Request); 
		req.setMq(this.mq);
		req.setToken(this.token);  
    	InvokeHelper.invokeAsync(pool, client, req, callback);
	}
	
	
	public String getMq() {
		return mq;
	} 
	public void setMq(String mq) {
		this.mq = mq;
	} 
	public String getToken() {
		return token;
	} 
	public void setToken(String token) {
		this.token = token;
	}
}
