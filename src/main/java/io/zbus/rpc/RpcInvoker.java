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
package io.zbus.rpc;

import java.io.IOException;

import io.zbus.mq.Message;
import io.zbus.mq.Message.MessageInvoker;
import io.zbus.net.Sync.ResultCallback;
import io.zbus.rpc.RpcCodec.Request;
import io.zbus.rpc.RpcCodec.Response;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class RpcInvoker{  
	private static final Logger log = LoggerFactory.getLogger(RpcInvoker.class); 
	
	private MessageInvoker messageInvoker; 
	private RpcCodec codec; 
	
	private String module = ""; 
	private String encoding = "UTF-8";
	private int timeout = 10000;  
	private boolean verbose = false;

	public RpcInvoker(MessageInvoker messageInvoker){
		this(messageInvoker, new JsonRpcCodec(), null);
	}
	
	public RpcInvoker(MessageInvoker messageInvoker, RpcCodec codec){
		this(messageInvoker, codec, null);
	}
	
	public RpcInvoker(MessageInvoker messageInvoker, RpcConfig config){
		this(messageInvoker, new JsonRpcCodec(), config);
	}
	
	public RpcInvoker(MessageInvoker messageInvoker, RpcCodec codec, RpcConfig config){ 
		this.messageInvoker = messageInvoker; 
		this.codec = codec;
		if(config != null){
			this.module = config.getModule();
			this.timeout = config.getTimeout(); 
			this.encoding = config.getEncoding();
			this.verbose = config.isVerbose();
		}
	}
	
	public <T> T invokeSync(Class<T> resultClass, String method, Object... args){
		Request request = new Request()
			.module(module)
			.method(method)  
			.params(args)
			.encoding(encoding);

		return invokeSync(resultClass, request);
	}
	
	public <T> T invokeSync(Class<T> resultClass, String method, Class<?>[] paramTypes, Object... args){
		Request request = new Request()
			.module(module)
			.method(method) 
			.paramTypes(paramTypes)
			.params(args)
			.encoding(encoding);
	
		return invokeSync(resultClass, request);
	} 
	
	public <T> T invokeSync(Class<T> resultClass, Request request){
		Response resp = invokeSync(request);
		try {
			@SuppressWarnings("unchecked")
			T res = (T)codec.convert(extractResult(resp), resultClass);
			return res;
		} catch (ClassNotFoundException e) { 
			throw new RpcException(e.getMessage(), e.getCause());
		}
	}
	
	
	public Object invokeSync(String method, Object... args) {	
		return invokeSync(method, null, args);
	}  
	
	public Object invokeSync(String method, Class<?>[] types, Object... args) {	
		Request req = new Request()
			.module(module)
			.method(method) 
			.paramTypes(types)
			.params(args)
			.encoding(encoding); 
		 
		Response resp = invokeSync(req);
		return extractResult(resp);
	} 
	
	public Response invokeSync(Request request){
		Message msgReq= null, msgRes = null;
		try {
			long start = System.currentTimeMillis();
			msgReq = codec.encodeRequest(request); 
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
					module, request.getMethod(), msgReq.toString());
			throw new RpcException(errorMsg);
		}
		
		return codec.decodeResponse(msgRes);
	}

	
	public <T> void invokeAsync(final Class<T> clazz, Request request,  final ResultCallback<T> callback){
		invokeAsync(request, new ResultCallback<Response>() { 
			@Override
			public void onReturn(Response resp) {  
				Object netObj = extractResult(resp);
				try {
					@SuppressWarnings("unchecked")
					T target = (T) codec.convert(netObj, clazz);
					callback.onReturn(target);
				} catch (ClassNotFoundException e) { 
					throw new RpcException(e.getMessage(), e.getCause());
				}
			}
		});
	}
	
	public void invokeAsync(Request request, final ResultCallback<Response> callback){ 
		final long start = System.currentTimeMillis();
		final Message msgReq = codec.encodeRequest(request); 
		if(isVerbose()){
			log.info("[REQ]: %s", msgReq);
		}  
		try {
			messageInvoker.invokeAsync(msgReq, new ResultCallback<Message>() {
				@Override
				public void onReturn(Message result) { 
					if(isVerbose()){
						long end = System.currentTimeMillis();
						log.info("[REP]: Time cost=%dms\n%s",(end-start), result); 
					} 
					Response resp = codec.decodeResponse(result);
					if(callback != null){
						callback.onReturn(resp);
					}
				}
			});
		} catch (IOException e) {
			throw new RpcException(e.getMessage(), e);
		}  
	}

	
	private Object extractResult(Response resp){
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
