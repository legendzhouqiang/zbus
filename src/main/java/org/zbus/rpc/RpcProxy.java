package org.zbus.rpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.log.Logger;
import org.zbus.mq.Broker;
 

public class RpcProxy {
	private static final Logger log = Logger.getLogger(RpcProxy.class);
	private static Constructor<RpcInvoker> rpcInvokerCtor;
	private Map<String,RpcInvoker> rpcInvokerCache = new ConcurrentHashMap<String, RpcInvoker>();
	private final Broker broker;
	static {
		try {
			rpcInvokerCtor = RpcInvoker.class.getConstructor(new Class[] {Rpc.class });
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}  
	
	public RpcProxy(Broker broker) {
		this.broker = broker;
	}
	
	/**
	 * mq=TRADE&&encoding=utf8
	 *
	 * parameters after ? got default values
	 * 
	 * encoding=UTF8
	 * module=
	 * timeout=10000
	 * token= 
	 * 
	 * @param api
	 * @param serviceURL
	 */
	public <T> T getService(Class<T> api, String serviceUrl) throws Exception {   
		RpcConfig config = parseRpcConfig(serviceUrl);
		return getService(api, config);
	} 
	
	@SuppressWarnings("unchecked")
	public <T> T getService(Class<T> api, RpcConfig config) throws Exception {   
		config.setBroker(this.broker);
		String mq = config.getMq(); 
		if(mq == null){
			throw new IllegalArgumentException("Missing argument mq");
		}
		String module = config.getModule();
		if(module == null ||module.trim().length()==0){
			module = api.getSimpleName();
			config.setModule(module);
		}
			
		String encoding = config.getEncoding();
		int timeout = config.getTimeout(); 
		
		String cacheKey = String.format(
				"mq=%s&&module=%s&&encoding=%s&&timeout=%d",
				mq, module, encoding, timeout);
		
		RpcInvoker rpcInvoker = rpcInvokerCache.get(cacheKey);
		Class<T>[] interfaces = new Class[] { api };
		if(rpcInvoker == null){
			Rpc rpc = new Rpc(config);
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
			if("mq".equals(key)){
				config.setMq(val);
			} else if("encoding".equals(key)){
				config.setEncoding(val);
			} else if("timeout".equals(key)){
				int timeout = 2500;//default
				try{ timeout = Integer.valueOf(val); }catch(Exception e){}
				config.setTimeout(timeout);
			} else if("topic".equals(key)){
				config.setTopic(val);
			} else if("verbose".equals(key)){
				boolean verbose = false;
				try{ verbose = Boolean.valueOf(val); }catch(Exception e){}
				config.setVerbose(verbose);
			} else if("mode".equals(key)){
				int mode = 0;
				try{ mode = Integer.valueOf(val); }catch(Exception e){}
				config.setMode(mode);
			} 
		}
		return config;
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
			return "RpcInvoker[" + this.rpc + "]";
		}
		return REMOTE_METHOD_CALL;
	} 
}