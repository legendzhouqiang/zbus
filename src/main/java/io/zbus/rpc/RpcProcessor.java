package io.zbus.rpc;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.rpc.annotation.Auth;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.Remote;
import io.zbus.rpc.doc.DocRender;

public class RpcProcessor {
	private static final Logger log = LoggerFactory.getLogger(RpcProcessor.class);
	
	public Map<String, Map<String, RpcMethod>> methodInfoTable = new HashMap<>(); //<module> => { method: => RpcMethod }

	protected Map<String, MethodInstance> methodTable = new HashMap<>();      //<module>:<method> => MethodInstance 
	
	protected String docUrlRoot = "/";
	protected boolean stackTraceEnabled = true;
	protected boolean methodPageEnabled = true; 
	protected boolean methodPageAuthEnabled = false;
	protected String methodPageModule = "index";
	
	protected RpcFilter beforeFilter;
	protected RpcFilter afterFilter;
	protected RpcFilter authFilter; 
	
	public void enableMethodPageModule() { 
		DocRender render = new DocRender(this, docUrlRoot);
		addModule(methodPageModule, render, false, methodPageAuthEnabled);
	}   
	
	
	public void addModule(Object service) {
		String module = defaultModuleName(service);
		addModule(module, service, true);
	}

	public void addModule(String module, Object service) {
		addModule(module, service, true);
	}
	
	public void addModule(String module, Object service, boolean enableModuleInfo) {
		addModule(module, service, enableModuleInfo, true);
	}
	
	public void addModule(String module, Object service, boolean enableModuleInfo, boolean defaultAuth) {
		this.addServiceMethods(module, service, defaultAuth);
		if(enableModuleInfo) {
			addModuleInfo(module, service);
		} 
	}  
	
	public void removeModule(Object service) { 
		String module = defaultModuleName(service);
		removeModule(module, service);  
	}

	public void removeModule(String module, Object service) {
		try {
			Method[] methods = service.getClass().getMethods();
			for (Method m : methods) {
				String methodName = m.getName();
				Remote cmd = m.getAnnotation(Remote.class);
				if (cmd != null) {
					methodName = cmd.id();
					if (cmd.exclude()) continue;
					if ("".equals(methodName)) {
						methodName = m.getName();
					}
				} 
				this.removeMethod(module, methodName);
			}
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		}
	} 
	
	public void addMethod(RpcMethod spec, MethodInvoker service) {
		MethodInstance mi = new MethodInstance(spec.method, service);
		mi.paramNames = spec.paramNames;
		
		String key = key(spec.module, spec.method);
		this.methodTable.put(key, mi);
		addMethodInfo(spec);
	}
	
	public void removeMethod(String module, String method) {
		String key = key(module, method);
		this.methodTable.remove(key); 
		Map<String, RpcMethod> table = this.methodInfoTable.get(module);
		if(table != null) {
			table.remove(method);
			if(table.isEmpty()) {
				this.methodInfoTable.remove(module);
			}
		}
	}
	
	private void addMethodInfo(RpcMethod spec) {
		Map<String, RpcMethod> moduleMethods = null;  
		if (methodInfoTable.containsKey(spec.module)) {
			moduleMethods = methodInfoTable.get(spec.module);
		} else {
			moduleMethods = new HashMap<>();
			methodInfoTable.put(spec.module, moduleMethods);
		}
		RpcMethod info = new RpcMethod(spec); 
		moduleMethods.put(spec.method, info); 
	}
	
	private void addModuleInfo(String module, Object service) {  
		Method[] methods = service.getClass().getMethods();   
		for (Method m : methods) {
			if (m.getDeclaringClass() == Object.class) continue;
			String method = m.getName();
			Remote cmd = m.getAnnotation(Remote.class);
			if (cmd != null) {
				method = cmd.id();
				if (cmd.exclude()) continue;
				if ("".equals(method)) {
					method = m.getName();
				}
			}
			RpcMethod spec = new RpcMethod(); 
			spec.module = module;
			spec.method = method;
			spec.returnType = m.getReturnType().getCanonicalName();
			List<String> paramTypes = new ArrayList<String>();
			for (Class<?> t : m.getParameterTypes()) {
				paramTypes.add(t.getCanonicalName());
			}
			spec.paramTypes = paramTypes; 
			
			Annotation[][] paramAnnos = m.getParameterAnnotations(); 
			int size = paramTypes.size(); 
			for(int i=0; i<size; i++) {
				Annotation[] annos = paramAnnos[i];
				for(Annotation annotation : annos) {
					if(Param.class.isAssignableFrom(annotation.getClass())) {
						Param p = (Param)annotation;
						spec.paramNames.add(p.value());
						break;
					}
				} 
			} 
			addMethodInfo(spec);
		}
	} 
	
	private void addServiceMethods(String module, Object service, boolean defaultAuth) { 
		try {
			Method[] methods = service.getClass().getMethods();
			boolean classAuthEnabled = defaultAuth;
			Auth classAuth = service.getClass().getAnnotation(Auth.class);
			if(classAuth != null) {
				classAuthEnabled = !classAuth.exclude();
			}
			for (Method m : methods) {
				if (m.getDeclaringClass() == Object.class) continue;

				String methodName = m.getName();
				Remote cmd = m.getAnnotation(Remote.class);
				if (cmd != null) {
					methodName = cmd.id();
					if (cmd.exclude()) continue;
					if ("".equals(methodName)) {
						methodName = m.getName();
					}
				} 
				
				Auth auth = m.getAnnotation(Auth.class);
				boolean authRequired = classAuthEnabled;
				if(auth != null) {
					authRequired = !auth.exclude();
				}

				m.setAccessible(true);
				MethodInstance mi = new MethodInstance(m, service); 
				mi.authRequired = authRequired;

				String key = key(module, methodName);
				if (this.methodTable.containsKey(key)) {
					log.debug(key + " overrided");
				}
				this.methodTable.put(key, mi); 
			}
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		}
	} 
	
	private MethodInstance matchMethod(Map<String, Object> req) {
		String module = (String)req.get(Protocol.MODULE);
		String method = (String)req.get(Protocol.METHOD);
		String key = key(module, method);
		return methodTable.get(key); 
	} 
	
	private String key(String module, String method) {
		return module + ":" + method;
	}
	
	public Map<String, Object> process(Map<String, Object> req) {  
		Map<String, Object> response = new HashMap<String, Object>();   
		try { 
			if (req == null) {
				req = new HashMap<>(); 
				req.put(Protocol.MODULE, "index");
				req.put(Protocol.METHOD, "index");
			}  
			
			String module = (String)req.get(Protocol.MODULE);
			String method = (String)req.get(Protocol.METHOD);  
			
			if(req.get(Protocol.PARAMS) == null){
				req.put(Protocol.PARAMS, new Object[0]);
			} 
			
			if (StrKit.isEmpty(module)) {
				req.put(Protocol.MODULE, "index");
			}
			if (StrKit.isEmpty(method)) {
				req.put(Protocol.METHOD, "index");
			}   
			
			if(beforeFilter != null) {
				boolean next = beforeFilter.doFilter(req, response);
				if(!next) return response;
			} 
			
			invoke(req, response);
			
			if(afterFilter != null) {
				afterFilter.doFilter(req, response);
			}
			 
		} catch (Throwable e) {
			response.put(Protocol.BODY, new RpcException(e.getMessage(), e.getCause(), false, stackTraceEnabled)); 
			response.put(Protocol.STATUS, 500);
		} finally {
			bindRequestResponse(req, response); 
			if(response.get(Protocol.STATUS) == null) {
				response.put(Protocol.STATUS, 200);
			}
		} 
		return response;
	}
	
	private void bindRequestResponse(Map<String, Object> request, Map<String, Object> response) {
		response.put(Protocol.ID, request.get(Protocol.ID)); //Id Match
	}
	
	@SuppressWarnings("unchecked")
	private Object[] getParams(Map<String, Object> req) {
		Object params = req.get(Protocol.PARAMS); 
		if(params instanceof List) {
			return ((List<Object>)params).toArray();
		}
		return (Object[])params;
	}
	
	@SuppressWarnings("unchecked")
	private void invoke(Map<String, Object> req, Map<String, Object> response) throws IllegalAccessException, IllegalArgumentException {   
		try {   
			MethodInstance target = matchMethod(req); 
			String module = (String)req.get(Protocol.MODULE);
			String method = (String)req.get(Protocol.METHOD); 
			Object[] params = getParams(req);
			
			if(target == null) {
				response.put(Protocol.STATUS,404);
				response.put(Protocol.BODY, String.format("module=%s, method=%s Not Found", module, method));
				return;
			}
			
			if(authFilter != null && target.authRequired) { 
				boolean next = authFilter.doFilter(req, response);
				if(!next) return;
			} 
			
			Object data = null;
			if(target.reflectedMethod != null) {
				Class<?>[] targetParamTypes = target.reflectedMethod.getParameterTypes();
				Object[] invokeParams = new Object[targetParamTypes.length]; 
				for (int i = 0; i < targetParamTypes.length; i++) { 
					if(i>=params.length) {
						invokeParams[i] = null;
					} else {
						invokeParams[i] = JsonKit.convert(params[i], targetParamTypes[i]);  
					}
				}
				data = target.reflectedMethod.invoke(target.instance, invokeParams);
				
			} else if(target.invokeBridge != null) {
				Map<String, Object> mapParams = new HashMap<>();  
				if(params != null) {
					if(params.length == 1 && params[0] instanceof Map) {
						mapParams = (Map<String, Object>)params[0]; 
					} else {
						for(int i=0;i <params.length; i++) {
							if(target.paramNames == null) break;
							if(i<target.paramNames.size()) {
								mapParams.put(target.paramNames.get(i), params[i]);
							}
						}
					}
				}
				data = target.invokeBridge.invoke(method, mapParams);
			}
			
			response.put(Protocol.BODY, data); 
			response.put(Protocol.STATUS, 200);
		} catch (InvocationTargetException e) {  
			Throwable t = e.getTargetException();
			if(t != null) {
				if(!stackTraceEnabled) {
					t.setStackTrace(new StackTraceElement[0]);
				}
			}
			response.put(Protocol.BODY, t);
			response.put(Protocol.STATUS, 500);
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
	
	public void setMethodPageAuthEnabled(boolean methodPageAuthEnabled) {
		this.methodPageAuthEnabled = methodPageAuthEnabled;
	}

	public String getMethodPageModule() {
		return methodPageModule;
	}

	public void setMethodPageModule(String methodPageModule) {
		this.methodPageModule = methodPageModule;
	}
	
	private static String defaultModuleName(Object service) {
		return service.getClass().getSimpleName();
	} 

	static class MethodMatchResult {
		MethodInstance method;
		boolean fullMatched;
	}
	
	static class MethodInstance {
		public String methodName; 
		public boolean authRequired = true;
		
		public Method reflectedMethod;
		public Object instance;  
		
		public MethodInvoker invokeBridge;   
		public List<String> paramNames; 
		
		public MethodInstance(Method reflectedMethod, Object instance) {
			this.reflectedMethod = reflectedMethod;
			this.instance = instance;
			this.methodName = this.reflectedMethod.getName();
		}
		
		public MethodInstance(String methodName, MethodInvoker invokeBridge) {  
			this.methodName = methodName;
			this.invokeBridge = invokeBridge;
		}
	}
}
