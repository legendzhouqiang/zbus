package org.zbus.client.rpc.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.logging.Logger;
import org.logging.LoggerFactory;
import org.remoting.RemotingClient;
import org.zbus.client.ClientPool;
 

public class JsonRpcProxy { //轻量级代理
	private static final Logger log = LoggerFactory.getLogger(JsonRpcProxy.class);
	private static Constructor<JsonRpcInvoker> jsonRpcInvokerCtor;
	private static Map<String,JsonRpcInvoker> jsonRpcInvokerCache = new ConcurrentHashMap<String, JsonRpcInvoker>();
	
	private ClientPool pool = null;
	private RemotingClient client = null;
	
	static {
		try {
			jsonRpcInvokerCtor = JsonRpcInvoker.class.getConstructor(new Class[] {JsonRpc.class });
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	} 
	
	public JsonRpcProxy(ClientPool agent){
		this.pool = agent;
	}
	
	public JsonRpcProxy(RemotingClient client){
		this.client = client;
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
		
		JsonRpcInvoker jsonRpcInvoker = jsonRpcInvokerCache.get(cacheKey);
		Class<T>[] interfaces = new Class[] { api };
		if(jsonRpcInvoker != null){
			return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, jsonRpcInvoker);
		}
		JsonRpc jsonRpc = null;
		if(pool != null){
			jsonRpc = new JsonRpc(pool, mq);
		} else {
			jsonRpc = new JsonRpc(client, mq);
		}
		
		if(module != null){
			jsonRpc.setModule(module);
		} 
		if(encoding != null) {
			jsonRpc.setEncoding(params.get("encoding"));
		}
		if(timeout != null) { 
			jsonRpc.setTimeout(Integer.valueOf(params.get("timeout")));
		}
		if (accessToken != null) { 
			jsonRpc.setToken(accessToken);
		} 
		
		jsonRpcInvoker = jsonRpcInvokerCtor.newInstance(jsonRpc); 
		jsonRpcInvokerCache.put(cacheKey, jsonRpcInvoker);
		
		return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, jsonRpcInvoker);
	} 
}

class JsonRpcInvoker implements InvocationHandler {  
	private JsonRpc jsonRpc; 
	private static final Object REMOTE_METHOD_CALL = new Object();

	public JsonRpcInvoker(JsonRpc jsonRpc) {
		this.jsonRpc = jsonRpc;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(args == null){
			args = new Object[0];
		}
		Object value = handleLocalMethod(proxy, method, args);
		if (value != REMOTE_METHOD_CALL) return value;
	
		return jsonRpc.invokeSyncWithType(method.getName(),method.getParameterTypes(), args);
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
			JsonRpcInvoker handler = (JsonRpcInvoker) Proxy.getInvocationHandler(value0);
			return new Boolean(this.jsonRpc.equals(handler.jsonRpc));
		} else if (methodName.equals("hashCode") && params.length == 0) {
			return new Integer(this.jsonRpc.hashCode());
		} else if (methodName.equals("toString") && params.length == 0) {
			return "RpcProxy[" + this.jsonRpc + "]";
		}
		return REMOTE_METHOD_CALL;
	} 
}