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
package org.zstacks.zbus.client.rpc;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.ZbusException;
import org.zstacks.zbus.client.service.Caller;
import org.zstacks.znet.Message;

public class Rpc extends Caller{  
	private static final Logger log = LoggerFactory.getLogger(Rpc.class);
	private static final Codec codec = new JsonCodec();
	public static final String DEFAULT_ENCODING = "UTF-8";  
	
	private String module = ""; 
	private String encoding = DEFAULT_ENCODING;
	private int timeout = 10000;  

	public Rpc(Broker broker, String mq) {
		super(broker, mq); 
	}
	
	public Rpc(RpcConfig config){
		super(config);
		this.module = config.getModule();
		this.timeout = config.getTimeout(); 
		this.encoding = config.getEncoding();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T invokeSync(Class<T> clazz, String method, Object... args){
		Object netObj = invokeSync(method, args);
		try {
			return (T) codec.normalize(netObj, clazz);
		} catch (ClassNotFoundException e) { 
			throw new ZbusException(e.getMessage(), e.getCause());
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T invokeSyncWithType(Class<T> clazz, String method, Class<?>[] types, Object... args){
		Object netObj = invokeSyncWithType(method, types, args);
		try {
			return (T) codec.normalize(netObj, clazz);
		} catch (ClassNotFoundException e) { 
			throw new ZbusException(e.getMessage(), e.getCause());
		}
	}
	
	public Object invokeSync(String method, Object... args) {
		return invokeSyncWithType(method, null, args);
	} 
	
	public Object invokeSyncWithType(String method, Class<?>[] types, Object... args) {	
		Request req = new Request();
		req.setModule(this.module);
		req.setMethod(method); 
		req.setParams(args); 
		req.assignParamTypes(types); 
		req.setEncoding(this.encoding);
		 
		Message msgReq= null, msgRes = null;
		try {
			msgReq = codec.encodeRequest(req);
			log.debug("Request: {}", msgReq);
			msgRes = this.invokeSync(msgReq, this.timeout); 
			log.debug("Response: {}", msgRes);
		} catch (IOException e) {
			throw new ZbusException(e.getMessage(), e);
		}
		
		if (msgRes == null) { 
			String errorMsg = String.format("MQ(%s)-module(%s)-method(%s) request timeout\n%s", 
					mq,  module, method, msgReq.toString());
			throw new ZbusException(errorMsg);
		}
		
		Response resp = codec.decodeResponse(msgRes);
		
		
		if(resp.getStackTrace() != null){
			Throwable error = resp.getError();
			if(error != null){
				if(error instanceof RuntimeException){
					throw (RuntimeException)error;
				}
				throw new ZbusException(error.getMessage(), error.getCause()); 
			} else {
				throw new ZbusException(resp.getStackTrace());
			}
		} 
		return resp.getResult();
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
	
	public Rpc module(String module) {
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
