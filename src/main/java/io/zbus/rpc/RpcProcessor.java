package io.zbus.rpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class RpcProcessor {
	private static final Logger log = LoggerFactory.getLogger(RpcProcessor.class);

	Map<String, MethodInstance> methods = new HashMap<String, MethodInstance>();
	Map<String, List<RpcMethod>> object2Methods = new HashMap<String, List<RpcMethod>>();

	String docUrlRoot = "/";
	boolean enableStackTrace = true;
	boolean enableMethodPage = true;

	public RpcProcessor(){
		addModule("index", new DocRender(this, docUrlRoot));
	}
	
	public void addModule(Object... services) {
		for (Object obj : services) {
			if (obj == null)
				continue; 
			addModule(obj.getClass().getSimpleName(), obj); 
		}
	}

	public void addModule(String module, Object... services) {
		for (Object service : services) {
			this.initCommandTable(module, service);
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
			removeModule(obj.getClass().getSimpleName(), obj); 
		}
	}

	public void removeModule(String module, Object... services) {
		for (Object service : services) {
			this.removeCommandTable(module, service);
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
				if (rm.getName().equals(method)) {
					rpcm = rm;
					break;
				}
			}
			if (rpcm != null) {
				if (!rpcm.modules.contains(module)) {
					rpcm.modules.add(module);
				}
			} else {
				List<String> modules = new ArrayList<String>();
				modules.add(module);
				rpcm = new RpcMethod();
				rpcm.setModules(modules);
				rpcm.setName(method);
				rpcm.setReturnType(m.getReturnType().getCanonicalName());
				List<String> paramTypes = new ArrayList<String>();
				for (Class<?> t : m.getParameterTypes()) {
					paramTypes.add(t.getCanonicalName());
				}
				rpcm.setParamTypes(paramTypes);
				rpcMethods.add(rpcm);
			}
		}
	}

	private void removeModuleInfo(Object service) {
		String serviceKey = service.getClass().getName();
		object2Methods.remove(serviceKey);
	}

	private void initCommandTable(String module, Object service) {
		addModuleInfo(module, service);

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

	static class MethodMatchResult {
		MethodInstance method;
		boolean fullMatched;
	}

	 
	private MethodMatchResult matchMethod(Request req) {
		StringBuilder sb = new StringBuilder(); 
		if(req.paramTypes != null){ 
			for (String type : req.paramTypes) {
				sb.append(type + ",");
			}
		} 
		String module = req.module;
		String method = req.method;
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

	private void checkParamTypes(MethodInstance target, Request req) {
		Class<?>[] targetParamTypes = target.method.getParameterTypes();
		int requiredLength = targetParamTypes.length;

		if (requiredLength != req.params.length) {
			String requiredParamTypeString = "";
			for (int i = 0; i < targetParamTypes.length; i++) {
				Class<?> paramType = targetParamTypes[i];
				requiredParamTypeString += paramType.getName();
				if (i < targetParamTypes.length - 1) {
					requiredParamTypeString += ", ";
				}
			}
			Object[] params = req.params;
			String gotParamsString = "";
			for (int i = 0; i < params.length; i++) {
				gotParamsString += params[i];
				if (i < params.length - 1) {
					gotParamsString += ", ";
				}
			}
			String errorMsg = String.format("Method:%s(%s), called with %s(%s)", target.method.getName(),
					requiredParamTypeString, target.method.getName(), gotParamsString);
			throw new IllegalArgumentException(errorMsg);
		}
	}

	public Response process(Request req) { 
		Response response = new Response();  
		try { 
			if (req == null) {
				req = new Request();
				req.method = "index";
				req.module = "index";
			}  
			if(req.params == null){
				req.params = new Object[0];
			}
			response.id = req.id;   //ID match
			response.attachment = req.attachment;
			
			if (StrKit.isEmpty(req.module)) {
				req.module = "index";
			}
			if (StrKit.isEmpty(req.method)) {
				req.method = "index";
			} 

			MethodMatchResult matchResult = matchMethod(req);
			MethodInstance target = matchResult.method;
			if (matchResult.fullMatched) {
				checkParamTypes(target, req);
			}

			Class<?>[] targetParamTypes = target.method.getParameterTypes();
			Object[] invokeParams = new Object[targetParamTypes.length];
			Object[] reqParams = req.params; 
			for (int i = 0; i < targetParamTypes.length; i++) { 
				invokeParams[i] = JsonKit.convert(reqParams[i], targetParamTypes[i]);  
			}
			response.result = target.method.invoke(target.instance, invokeParams); 
		} catch (InvocationTargetException e) { 
			response.error = e.getTargetException();
		} catch (Throwable e) {
			response.error = new RpcException(e.getMessage(), e.getCause(), false, enableStackTrace); 
		} 
		return response;
	}

	public boolean isEnableStackTrace() {
		return enableStackTrace;
	}

	public void setEnableStackTrace(boolean enableStackTrace) {
		this.enableStackTrace = enableStackTrace;
	}

	public boolean isEnableMethodPage() {
		return enableMethodPage;
	}

	public void setEnableMethodPage(boolean enableMethodPage) {
		this.enableMethodPage = enableMethodPage;
	}

	private static class MethodInstance {
		public Method method;
		public Object instance;

		public MethodInstance(Method method, Object instance) {
			this.method = method;
			this.instance = instance;
		}
	}

	public static class RpcMethod {
		List<String> modules = new ArrayList<String>();
		String name;
		List<String> paramTypes = new ArrayList<String>();
		String returnType;

		public List<String> getModules() {
			return modules;
		}

		public void setModules(List<String> modules) {
			this.modules = modules;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<String> getParamTypes() {
			return paramTypes;
		}

		public void setParamTypes(List<String> paramTypes) {
			this.paramTypes = paramTypes;
		}

		public String getReturnType() {
			return returnType;
		}

		public void setReturnType(String returnType) {
			this.returnType = returnType;
		}
	}
}
