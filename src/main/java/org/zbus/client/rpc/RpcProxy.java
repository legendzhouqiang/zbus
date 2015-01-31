package org.zbus.client.rpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.client.Broker;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
 
/**
 * 
 * 
 * @author 洪磊明(rushmore)
 *
 */
public class RpcProxy {
	private static final Logger log = LoggerFactory.getLogger(RpcProxy.class);
	private static Constructor<RpcInvoker> rpcInvokerCtor;
	private static Map<String,RpcInvoker> rpcInvokerCache = new ConcurrentHashMap<String, RpcInvoker>();
	
	private final Broker broker;
	
	static {
		try {
			rpcInvokerCtor = RpcInvoker.class.getConstructor(new Class[] {Rpc.class });
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	} 
	
	public RpcProxy(Broker broker){
		this.broker = broker;
	}
	
	private static Map<String, String> parseKeyValues(String kvstring) {
		Map<String, String> res = new HashMap<String, String>();
		String[] parts = kvstring.split("\\&\\&");
		for(String kv : parts){
			String[] kvp = kv.split("=");
			String key = kvp[0].trim();
			String val = "";
			if(kvp.length>1){
				val = kvp[1].trim();
			} 
			res.put(key, val);
		}
		return res;
	} 
	
	/**
	 * mq=TRADE&&encoding=utf8
	 * 
	 * encoding=UTF8
	 * module=
	 * timeout=10000
	 * token= 
	 * 
	 * @param api 
	 */
	public <T> T getService(Class<T> api, String params) throws Exception { 
		Map<String, String> kvs = parseKeyValues(params);
		return getService(api, kvs);
	} 
	
	public <T> T getService(Class<T> api, String mq, Map<String, String> params) throws Exception { 
		params.put("mq", mq);
		return getService(api, params);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getService(Class<T> api, Map<String, String> params) throws Exception {  
		String mq = params.get("mq"); 
		if(mq == null){
			throw new IllegalArgumentException("Missing argument mq");
		}
		String module = params.get("module");
		if(module == null){
			module = api.getSimpleName();
		}
			
		String encoding = params.get("encoding");
		String timeout = params.get("timeout");
		String accessToken = params.get("accessToken");
		
		String cacheKey = String.format("mq=%s&&module=%s&&encoding=%s&&timeout=%s&&accessToken=%s",
				mq, module, encoding, timeout, accessToken);
		
		RpcInvoker rpcInvoker = rpcInvokerCache.get(cacheKey);
		Class<T>[] interfaces = new Class[] { api };
		if(rpcInvoker == null){
			Rpc rpc = new Rpc(broker, mq);
			if(module != null){
				rpc.setModule(module);
			} 
			if(encoding != null) {
				rpc.setEncoding(params.get("encoding"));
			}
			if(timeout != null) { 
				rpc.setTimeout(Integer.valueOf(params.get("timeout")));
			}
			if (accessToken != null) { 
				rpc.setAccessToken(accessToken);
			} 
			
			rpcInvoker = rpcInvokerCtor.newInstance(rpc); 
			rpcInvokerCache.put(cacheKey, rpcInvoker); 
		}
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return (T) Proxy.newProxyInstance(classLoader, interfaces, rpcInvoker);
	} 
}

class RpcInvoker implements InvocationHandler {  
	private Rpc rpc; 
	private static final Object REMOTE_METHOD_CALL = new Object();

	public RpcInvoker(Rpc rpc) {
		this.rpc = rpc;
	}
	
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(args == null){
			args = new Object[0];
		}
		Object value = handleLocalMethod(proxy, method, args);
		if (value != REMOTE_METHOD_CALL) return value; 
		Class<?> returnType = method.getReturnType(); 
		return rpc.invokeSyncWithType(returnType, method.getName(),method.getParameterTypes(), args);
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
			RpcInvoker handler = (RpcInvoker) Proxy.getInvocationHandler(value0);
			return new Boolean(this.rpc.equals(handler.rpc));
		} else if (methodName.equals("hashCode") && params.length == 0) {
			return new Integer(this.rpc.hashCode());
		} else if (methodName.equals("toString") && params.length == 0) {
			return "RpcProxy[" + this.rpc + "]";
		}
		return REMOTE_METHOD_CALL;
	} 
}