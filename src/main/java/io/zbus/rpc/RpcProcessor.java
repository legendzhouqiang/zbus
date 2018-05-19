package io.zbus.rpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.rpc.annotation.Remote;

public class RpcProcessor {
	private static final Logger log = LoggerFactory.getLogger(RpcProcessor.class);

	Map<String, MethodInstance> methods = new HashMap<>();
	Map<String, List<RpcMethod>> object2Methods = new HashMap<>();
	
	String docUrlRoot = "/";
	boolean stackTraceEnabled = true;
	boolean methodPageEnabled = true; 
	String methodPageModule = "index";
	
	protected RpcFilter beforeFilter;
	protected RpcFilter afterFilter;
	protected RpcFilter authFilter; 
	
	public void enableMethodPageModule() {
		Object[] services = new Object[] {new DocRender(this, docUrlRoot)};
		addModule(false, methodPageModule, services);
	}
	
	private static String defaultModuleName(Object service) {
		return service.getClass().getSimpleName();
	} 

	public void addModule(String module, Object... services) {
		addModule(true, module, services);
	}
	
	public void addModule(boolean enableModuleInfo, String module, Object... services) {
		for (Object service : services) {
			this.initCommandTable(module, service);
			if(enableModuleInfo) {
				addModuleInfo(module, service);
			}
		}
	}

	public void addModule(Class<?>... clazz) {
		Object[] services = new Object[clazz.length];
		for (int i = 0; i < clazz.length; i++) {
			Class<?> c = clazz[i];
			try {
				services[i] = c.newInstance();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		addModule(services);
	}
	
	public void addModule(Object... services) {
		for (Object obj : services) {
			if (obj == null)
				continue; 
			String module = defaultModuleName(obj);
			addModule(module, obj); 
		}
	}

	public void addModule(String module, Class<?>... clazz) {
		Object[] services = new Object[clazz.length];
		for (int i = 0; i < clazz.length; i++) {
			Class<?> c = clazz[i];
			try {
				services[i] = c.newInstance();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		addModule(module, services);
	}

	public void removeModule(Object... services) {
		for (Object obj : services) { 
			String module = defaultModuleName(obj);
			removeModule(module, obj); 
		}
	}

	public void removeModule(String module, Object... services) {
		for (Object service : services) {
			this.removeCommandTable(module, service);
		}
	} 
	
	public void addMethod(RpcMethod spec, GenericInvocation service) {
		MethodInstance mi = new MethodInstance(spec.method, service);
		mi.paramNames = spec.paramNames;
		
		String key = key(spec.module, spec.method);
		this.methods.put(key, mi);
		addMethodInfo(spec, service);
	}
	
	private void addMethodInfo(RpcMethod spec, GenericInvocation service) {
		List<RpcMethod> rpcMethods = null;
		String serviceKey = service.getClass().getCanonicalName();
		if (object2Methods.containsKey(serviceKey)) {
			rpcMethods = object2Methods.get(serviceKey);
		} else {
			rpcMethods = new ArrayList<RpcMethod>();
			object2Methods.put(serviceKey, rpcMethods);
		}
		
		RpcMethod rpcm = null;
		for (RpcMethod rm : rpcMethods) {
			if (spec.method.equals(rm.method)) {
				rpcm = rm;
				break;
			}
		}
		if (rpcm != null) {
			rpcm.module = spec.module; 
		} else {  
			rpcm = new RpcMethod();
			rpcm.module = spec.module;
			rpcm.method = spec.method;
			rpcm.returnType = spec.returnType; 
			rpcm.paramNames = spec.paramNames;
			rpcm.paramTypes = spec.paramTypes;
			rpcMethods.add(rpcm);
		} 
	}
	
	private void addModuleInfo(String module, Object service) {
		List<RpcMethod> rpcMethods = null;
		String serviceKey = service.getClass().getCanonicalName();
		if (object2Methods.containsKey(serviceKey)) {
			rpcMethods = object2Methods.get(serviceKey);
		} else {
			rpcMethods = new ArrayList<RpcMethod>();
			object2Methods.put(serviceKey, rpcMethods);
		}

		Method[] methods = service.getClass().getMethods();
		
		Arrays.sort(methods, (a, b)->{
			return a.getName().compareTo(b.getName()); 
		});
		
		for (Method m : methods) {
			if (m.getDeclaringClass() == Object.class)
				continue;
			String method = m.getName();
			Remote cmd = m.getAnnotation(Remote.class);
			if (cmd != null) {
				method = cmd.id();
				if (cmd.exclude())
					continue;
				if ("".equals(method)) {
					method = m.getName();
				}
			}
			RpcMethod rpcm = null;
			for (RpcMethod rm : rpcMethods) {
				if (method.equals(rm.method)) {
					rpcm = rm;
					break;
				}
			}
			if (rpcm != null) {
				rpcm.module = module; 
			} else {  
				rpcm = new RpcMethod();
				rpcm.module = module;
				rpcm.method = method;
				rpcm.returnType = m.getReturnType().getCanonicalName();
				List<String> paramTypes = new ArrayList<String>();
				for (Class<?> t : m.getParameterTypes()) {
					paramTypes.add(t.getCanonicalName());
				}
				rpcm.paramTypes = paramTypes;
				rpcMethods.add(rpcm);
			}
		}
	}

	private void removeModuleInfo(Object service) {
		String serviceKey = service.getClass().getName();
		object2Methods.remove(serviceKey);
	}

	private void initCommandTable(String module, Object service) { 
		try {
			Method[] methods = service.getClass().getMethods();
			for (Method m : methods) {
				if (m.getDeclaringClass() == Object.class)
					continue;

				String method = m.getName();
				Remote cmd = m.getAnnotation(Remote.class);
				if (cmd != null) {
					method = cmd.id();
					if (cmd.exclude())
						continue;
					if ("".equals(method)) {
						method = m.getName();
					}
				}

				m.setAccessible(true);
				MethodInstance mi = new MethodInstance(m, service); 

				String[] keys = paramSignature(module, m);
				for (String key : keys) {
					if (this.methods.containsKey(key)) {
						log.debug(key + " overrided");
					}
					this.methods.put(key, mi);
				}
			}
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void removeCommandTable(String module, Object service) {
		removeModuleInfo(service);

		try {
			Method[] methods = service.getClass().getMethods();
			for (Method m : methods) {
				String method = m.getName();
				Remote cmd = m.getAnnotation(Remote.class);
				if (cmd != null) {
					method = cmd.id();
					if (cmd.exclude())
						continue;
					if ("".equals(method)) {
						method = m.getName();
					}
				}
				String[] keys = paramSignature(module, m);
				for (String key : keys) {
					this.methods.remove(key);
				}
			}
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		}
	} 
	
	private MethodMatchResult matchMethod(Request req) {
		StringBuilder sb = new StringBuilder(); 
		if(req.getParamTypes() != null){ 
			for (String type : req.getParamTypes()) {
				sb.append(type + ",");
			}
		} else if(req.getParams() != null) {
			for(Object param : req.getParams()) {
				sb.append(param.getClass().getName() + ",");
			}
		}
		String module = req.getModule();
		String method = req.getMethod();
		String key = module + ":" + method + ":" + sb.toString();
		String key2 = module + ":" + method;

		MethodMatchResult result = new MethodMatchResult();
		if (this.methods.containsKey(key)) {
			result.method = this.methods.get(key);
			result.fullMatched = true;
			return result;
		} else {
			if (this.methods.containsKey(key2)) {
				result.method = this.methods.get(key2);
				result.fullMatched = false;
				return result;
			}
			String errorMsg = String.format("%s:%s Not Found", module, method);
			throw new IllegalArgumentException(errorMsg);
		}
	}

	private String[] paramSignature(String module, Method m) {
		Class<?>[] paramTypes = m.getParameterTypes();
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		for (int i = 0; i < paramTypes.length; i++) {
			sb.append(paramTypes[i].getSimpleName() + ",");
			sb2.append(paramTypes[i].getName() + ",");
		}

		String key = module + ":" + m.getName() + ":" + sb.toString();
		String key2 = module + ":" + m.getName() + ":" + sb2.toString();
		String key3 = module + ":" + m.getName();
		if (key.equals(key2)) {
			return new String[] { key, key3 };
		}
		return new String[] { key, key2, key3 };
	} 
	
	private String key(String module, String method) {
		return module + ":" + method;
	}
	
	public Response process(Request req) {  
		Response response = new Response();  
		try { 
			if (req == null) {
				req = new Request();
				req.setMethod("index");
				req.setModule("index");
			}  
			if(req.getParams() == null){
				req.setParams(new Object[0]);
			} 
			
			if (StrKit.isEmpty(req.getModule())) {
				req.setModule("index");
			}
			if (StrKit.isEmpty(req.getMethod())) {
				req.setMethod("index");
			}   
			
			if(beforeFilter != null) {
				boolean next = beforeFilter.doFilter(req, response);
				if(!next) return response;
			}
			
			if(authFilter != null) {
				boolean next = authFilter.doFilter(req, response);
				if(!next) return response; 
			} 
			
			invoke(req, response);
			
			if(afterFilter != null) {
				afterFilter.doFilter(req, response);
			}
			 
		} catch (Throwable e) {
			response.setData(new RpcException(e.getMessage(), e.getCause(), false, stackTraceEnabled)); 
			response.setStatus(500);
		} finally {
			bindRequestResponse(req, response); 
			if(response.getStatus() == null) {
				response.setStatus(200);
			}
		} 
		return response;
	}
	
	private void bindRequestResponse(Request request, Response response) {
		response.setId(request.getId()); //Id Match
	}
	
	@SuppressWarnings("unchecked")
	private void invoke(Request req, Response response) throws IllegalAccessException, IllegalArgumentException {   
		try {   
			MethodMatchResult matchResult = matchMethod(req);
			
			MethodInstance target = matchResult.method;  
			
			Object data = null;
			if(target.reflectedMethod != null) {
				Class<?>[] targetParamTypes = target.reflectedMethod.getParameterTypes();
				Object[] invokeParams = new Object[targetParamTypes.length];
				List<Object> reqParams = req.getParams(); 
				for (int i = 0; i < targetParamTypes.length; i++) { 
					if(i>=reqParams.size()) {
						invokeParams[i] = null;
					} else {
						invokeParams[i] = JsonKit.convert(reqParams.get(i), targetParamTypes[i]);  
					}
				}
				data = target.reflectedMethod.invoke(target.instance, invokeParams);
				
			} else if(target.genericInvocation != null) {
				Map<String, Object> mapParams = new HashMap<>(); 
				List<Object> paramList = req.getParams();
				if(paramList != null) {
					if(paramList.size() == 1 && paramList.get(0) instanceof Map) {
						mapParams = (Map<String, Object>)paramList.get(0); 
					} else {
						for(int i=0;i <paramList.size(); i++) {
							if(target.paramNames == null) break;
							if(i<target.paramNames.size()) {
								mapParams.put(target.paramNames.get(i), paramList.get(i));
							}
						}
					}
				}
				data = target.genericInvocation.invoke(req.getMethod(), mapParams);
			}
			
			response.setData(data); 
			response.setStatus(200);
		} catch (InvocationTargetException e) {  
			Throwable t = e.getTargetException();
			if(t != null) {
				if(!stackTraceEnabled) {
					t.setStackTrace(new StackTraceElement[0]);
				}
			}
			response.setData(t);
			response.setStatus(500);
		} 
	}

	public void setBeforeFilter(RpcFilter beforeFilter) {
		this.beforeFilter = beforeFilter;
	} 

	public void setAfterFilter(RpcFilter afterFilter) {
		this.afterFilter = afterFilter;
	} 

	public void setAuthFilter(RpcFilter authFilter) {
		this.authFilter = authFilter;
	} 

	public boolean isStackTraceEnabled() {
		return stackTraceEnabled;
	}

	public void setStackTraceEnabled(boolean stackTraceEnabled) {
		this.stackTraceEnabled = stackTraceEnabled;
	}

	public boolean isMethodPageEnabled() {
		return methodPageEnabled;
	}

	public void setMethodPageEnabled(boolean methodPageEnabled) {
		this.methodPageEnabled = methodPageEnabled;
	}

	public String getMethodPageModule() {
		return methodPageModule;
	}

	public void setMethodPageModule(String methodPageModule) {
		this.methodPageModule = methodPageModule;
	}

	static class MethodMatchResult {
		MethodInstance method;
		boolean fullMatched;
	}
	
	static class MethodInstance {
		public String methodName; 
		
		public Method reflectedMethod;
		public Object instance;  
		
		public GenericInvocation genericInvocation;   
		public List<String> paramNames; 
		
		public MethodInstance(Method reflectedMethod, Object instance) {
			this.reflectedMethod = reflectedMethod;
			this.instance = instance;
			this.methodName = this.reflectedMethod.getName();
		}
		
		public MethodInstance(String methodName, GenericInvocation genericInvocation) {  
			this.methodName = methodName;
			this.genericInvocation = genericInvocation;
		}
	}
}
