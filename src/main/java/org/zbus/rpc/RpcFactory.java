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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.net.http.Message.MessageInvoker;


public class RpcFactory { 
	private static final Logger log = LoggerFactory.getLogger(RpcFactory.class);
	private static Constructor<RpcInvocationHandler> rpcInvokerCtor;
	private Map<String,RpcInvocationHandler> rpcInvokerCache = new ConcurrentHashMap<String, RpcInvocationHandler>();
	private final MessageInvoker messageInvoker; 
	static {
		try {
			rpcInvokerCtor = RpcInvocationHandler.class.getConstructor(new Class[] {RpcInvoker.class });
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}  
	
	public RpcFactory(MessageInvoker messageInvoker) {
		this.messageInvoker = messageInvoker;
	}
	public <T> T getService(Class<T> api) throws Exception{
		return getService(api, new RpcConfig());
	}
	public <T> T getService(Class<T> api, String serviceUrl) throws Exception {   
		RpcConfig config = parseRpcConfig(serviceUrl);  
		return getService(api, config);
	} 
	
	@SuppressWarnings("unchecked")
	public <T> T getService(Class<T> api, RpcConfig config) throws Exception {   
		String module = config.getModule();
		if(module == null ||module.trim().length()==0){
			module = api.getSimpleName();
			config.setModule(module);
		}
			
		String encoding = config.getEncoding();
		int timeout = config.getTimeout(); 
		
		String cacheKey = String.format("module=%s&&encoding=%s&&timeout=%d", module, encoding, timeout);
		
		RpcInvocationHandler rpcInvoker = rpcInvokerCache.get(cacheKey);
		Class<T>[] interfaces = new Class[] { api };
		if(rpcInvoker == null){
			RpcInvoker rpc = new RpcInvoker(messageInvoker, config);
			rpcInvoker = rpcInvokerCtor.newInstance(rpc); 
			rpcInvokerCache.put(cacheKey, rpcInvoker); 
		}
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return (T) Proxy.newProxyInstance(classLoader, interfaces, rpcInvoker);
	} 
	 
	private static RpcConfig parseRpcConfig(String kvstring) {
		RpcConfig config = new RpcConfig();  
		String[] parts = kvstring.split("\\&");
		for(String kv : parts){
			String[] kvp = kv.split("=");
			String key = kvp[0].trim();
			String val = "";
			if(kvp.length>1){
				val = kvp[1].trim();
			} 
			if("module".equals(key)){
				config.setModule(val);
			} else if("encoding".equals(key)){
				config.setEncoding(val);
			} else if("timeout".equals(key)){
				int timeout = 2500;//default
				try{ timeout = Integer.valueOf(val); }catch(Exception e){}
				config.setTimeout(timeout);
			} else if("verbose".equals(key)){
				boolean verbose = false;
				try{ verbose = Boolean.valueOf(val); }catch(Exception e){}
				config.setVerbose(verbose);
			} 
		}
		return config;
	} 
}

class RpcInvocationHandler implements InvocationHandler {  
	private RpcInvoker rpc; 
	private static final Object REMOTE_METHOD_CALL = new Object();

	public RpcInvocationHandler(RpcInvoker rpc) {
		this.rpc = rpc;
	}
	
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(args == null){
			args = new Object[0];
		}
		Object value = handleLocalMethod(proxy, method, args);
		if (value != REMOTE_METHOD_CALL) return value; 
		Class<?> returnType = method.getReturnType(); 
		return rpc.invokeSync(returnType, method.getName(),method.getParameterTypes(), args);
	}

	protected Object handleLocalMethod(Object proxy, Method method,
			Object[] args) throws Throwable {
		String methodName = method.getName();
		Class<?>[] params = method.getParameterTypes();

		if (methodName.equals("equals") && params.length == 1
				&& params[0].equals(Object.class)) {
			Object value0 = args[0];
			if (value0 == null || !Proxy.isProxyClass(value0.getClass()))
				return new Boolean(false);
			RpcInvocationHandler handler = (RpcInvocationHandler) Proxy.getInvocationHandler(value0);
			return new Boolean(this.rpc.equals(handler.rpc));
		} else if (methodName.equals("hashCode") && params.length == 0) {
			return new Integer(this.rpc.hashCode());
		} else if (methodName.equals("toString") && params.length == 0) {
			return "RpcInvocationHandler[" + this.rpc + "]";
		}
		return REMOTE_METHOD_CALL;
	} 
}