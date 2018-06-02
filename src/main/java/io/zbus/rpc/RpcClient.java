package io.zbus.rpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.zbus.kit.JsonKit;
import io.zbus.transport.http.WebsocketClient;
import okhttp3.OkHttpClient; 

public class RpcClient extends WebsocketClient {   
	public RpcClient(String address) {  
		super(address);
	}  
	
	public RpcClient(String address, OkHttpClient httpClient) {
		super(address, httpClient); 
	}  
	 
	private static <T> T parseResult(Response resp, Class<T> clazz) { 
		Object data = resp.getBody();
		if(resp.getStatus() != 200){
			if(data instanceof RuntimeException){
				throw (RuntimeException)data;
			} else {
				throw new RpcException(data.toString());
			}
		} 
		try { 
			return (T) JsonKit.convert(data, clazz); 
		} catch (Exception e) { 
			throw new RpcException(e.getMessage(), e.getCause());
		}
	}  
	
	
	@SuppressWarnings("unchecked")
	public <T> T createProxy(Class<T> clazz, String module){  
		Constructor<RpcInvocationHandler> rpcInvokerCtor;
		try {
			rpcInvokerCtor = RpcInvocationHandler.class.getConstructor(new Class[] {RpcClient.class, String.class }); 
			RpcInvocationHandler rpcInvokerHandler = rpcInvokerCtor.newInstance(this, module); 
			Class<T>[] interfaces = new Class[] { clazz }; 
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			return (T) Proxy.newProxyInstance(classLoader, interfaces, rpcInvokerHandler);
		} catch (Exception e) { 
			throw new RpcException(e);
		}   
	}  
	
	
	public static class RpcInvocationHandler implements InvocationHandler {  
		private RpcClient rpc; 
		private String module;
		private static final Object REMOTE_METHOD_CALL = new Object();

		public RpcInvocationHandler(RpcClient rpc, String module) {
			this.rpc = rpc;
			this.module = module;
		}
		
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if(args == null){
				args = new Object[0];
			}
			Object value = handleLocalMethod(proxy, method, args);
			if (value != REMOTE_METHOD_CALL) return value; 
			 
			Request request = new Request();
			request.setModule(module);
			request.setMethod(method.getName());  
			request.setParams(args);
			
			Response resp = new Response(rpc.invoke(request));
			return parseResult(resp, method.getReturnType());
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
}
