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
package org.zbus.rpc;

import java.io.IOException;

import org.zbus.log.Logger;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageInvoker;
import org.zbus.rpc.RpcCodec.Request;
import org.zbus.rpc.RpcCodec.Response;

public class RpcInvoker{  
	private static final Logger log = Logger.getLogger(RpcInvoker.class);
	private static final RpcCodec codec = new JsonRpcCodec(); 
	
	private MessageInvoker messageInvoker; 
	
	private String module = ""; 
	private String encoding = "UTF-8";
	private int timeout = 10000;  
	private boolean verbose = false;

	public RpcInvoker(MessageInvoker messageInvoker){
		this.messageInvoker = messageInvoker;
	}
	
	public RpcInvoker(MessageInvoker messageInvoker, RpcConfig config){ 
		this.messageInvoker = messageInvoker; 
		this.module = config.getModule();
		this.timeout = config.getTimeout(); 
		this.encoding = config.getEncoding();
		this.verbose = config.isVerbose();
	}
	
	public <T> T invokeSync(Class<T> clazz, String method, Object... args){
		return invokeSync(clazz, method, (Class<?>[])null, args);
	}
	
	public Object invokeSync(String method, Object... args) {
		return invokeSync(method, (Class<?>[])null, args);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T invokeSync(Class<T> clazz, String method, Class<?>[] types, Object... args){
		Object netObj = invokeSync(method, types, args);
		try {
			return (T) codec.normalize(netObj, clazz);
		} catch (ClassNotFoundException e) { 
			throw new RpcException(e.getMessage(), e.getCause());
		}
	}

	public Object invokeSync(String method, Class<?>[] types, Object... args) {	
		Request req = new Request();
		req.setModule(this.module);
		req.setMethod(method); 
		req.setParams(args); 
		req.assignParamTypes(types); 
		req.setEncoding(this.encoding);
		 
		Message msgReq= null, msgRes = null;
		try {
			long start = System.currentTimeMillis();
			msgReq = codec.encodeRequest(req); 
			if(isVerbose()){
				log.info("[REQ]: %s", msgReq);
			} 
			
			msgRes = messageInvoker.invokeSync(msgReq, this.timeout); 
			
			if(isVerbose()){
				long end = System.currentTimeMillis();
				log.info("[REP]: Time cost=%dms\n%s",(end-start), msgRes);
			} 
			
		} catch (IOException e) {
			throw new RpcException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new RpcException(e.getMessage(), e);
		}
		
		if (msgRes == null) { 
			String errorMsg = String.format("module(%s)-method(%s) request timeout\n%s", 
					module, method, msgReq.toString());
			throw new RpcException(errorMsg);
		}
		
		Response resp = codec.decodeResponse(msgRes);
		
		
		if(resp.getStackTrace() != null){
			Throwable error = resp.getError();
			if(error != null){
				if(error instanceof RuntimeException){
					throw (RuntimeException)error;
				}
				throw new RpcException(error.getMessage(), error.getCause()); 
			} else {
				throw new RpcException(resp.getStackTrace());
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
	
	public RpcInvoker module(String module) {
		this.module = module;
		return this;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	} 
	
}
