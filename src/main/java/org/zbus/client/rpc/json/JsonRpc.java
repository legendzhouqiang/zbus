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
package org.zbus.client.rpc.json;


import java.io.IOException;

import org.zbus.client.ClientPool;
import org.zbus.client.ZbusException;
import org.zbus.client.rpc.Rpc;
import org.zbus.logging.Logger;
import org.zbus.logging.LoggerFactory;
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;

public class JsonRpc extends Rpc{  
	private static final Logger log = LoggerFactory.getLogger(JsonRpc.class);
	
	public static final String DEFAULT_ENCODING = "UTF-8";  
	
	private String module = ""; 
	private String encoding = DEFAULT_ENCODING;
	private int timeout = 10000;  


	public JsonRpc(ClientPool pool, String mq) {
		super(pool, mq); 
	}
	
	public JsonRpc(RemotingClient client, String mq) {
		super(client, mq); 
	}

	
	public <T> T invokeSync(String method, Object... args) {
		return invokeSyncWithType(method, null, args);
	} 
	
	public <T> T invokeSyncWithType(String method, Class<?>[] types, Object... args) {	
		JsonRequest req = new JsonRequest();
		req.setModule(this.module);
		req.setMethod(method); 
		req.setParams(args); 
		req.setParamTypes(types); 
		Message msg = JsonHelper.packJsonRequest(req);
		
		try {
			log.debug("Request: %s", msg);
			msg = this.invokeSync(msg, this.timeout); 
			log.debug("Reply: %s", msg);
		} catch (IOException e) {
			throw new ZbusException(e.getMessage(), e);
		}
		
		if (msg == null) { 
			throw new ZbusException("json rpc request timeout");
		}
		
		return JsonHelper.unpackReplyObject(msg, this.encoding);
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}
	
	public JsonRpc module(String module) {
		this.module = module;
		return this;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	
}
