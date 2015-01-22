/************************************************************************
 *  Copyright (c) 2011-2012 HONG LEIMING.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 ***************************************************************************/
package org.zbus.client.rpc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.zbus.client.ServiceHandler;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.Message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

class MethodInstance{
	public Method method;
	public Object instance;
	
	public MethodInstance(Method method, Object instance){
		this.method = method;
		this.instance = instance;
	}

	@Override
	public String toString() {
		return "MethodInstance [method=" + method + ", instance=" + instance + "]";
	} 
}

public class RpcServiceHandler implements ServiceHandler {
	private static final Logger log = LoggerFactory.getLogger(RpcServiceHandler.class);
	
	private static final String DEFAULT_ENCODING = "UTF-8"; 
	private Map<String,MethodInstance> methods = new HashMap<String, MethodInstance>();
	
	
	public void addModule(Class<?> clazz, Object... services){
		addModule(clazz.getSimpleName(), services);
	}
	
	public void addModule(String module, Object... services){
		for(Object service: services){
			this.initCommandTable(module, service);
		}
	}
	
	private void initCommandTable(String module, Object service){
		Class<?>[] classes = new Class<?>[service.getClass().getInterfaces().length+1];
		classes[0] = service.getClass(); 
		for(int i=1; i<classes.length; i++){
			classes[i] = service.getClass().getInterfaces()[i-1];
		}
		try { 
			for(Class<?> clazz : classes){
				Method [] methods = clazz.getMethods(); 
				for (Method m : methods) { 
					Remote cmd = m.getAnnotation(Remote.class);
					if(cmd != null){
						String paramMD5 = ""; 
						Class<?>[] paramTypes = m.getParameterTypes();
						StringBuilder sb = new StringBuilder();
						for(int i=0;i<paramTypes.length;i++){ 
							sb.append(paramTypes[i].getCanonicalName());
						}
						paramMD5 = sb.toString();
						
						String method = cmd.id();
						if("".equals(method)){
							method = m.getName();
						}  
						String key = module + ":" + method+":"+paramMD5; 
						String key2 = module + ":" + method;
						if(this.methods.containsKey(key)){
							log.error(key + " duplicated"); 
						} else {
							log.debug("register "+service.getClass().getSimpleName()+"\t" + key);
							log.debug("register "+service.getClass().getSimpleName()+"\t"  + key2);
						}
						m.setAccessible(true);
						MethodInstance mi = new MethodInstance(m, service);
						this.methods.put(key, mi);  
						this.methods.put(key2, mi);  
					}
				} 
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}   
	}
	
	
	public Message handleJsonRequest(byte[] jsonData, String charsetName){  
		Throwable error = null;
		Object result = null; 
		JSONObject req = null;
		String module = "";
		String method = null; 
		JSONArray args = null;
		MethodInstance target = null; 
		JSONArray paramTypes = null;
		String paramMD5 = "";
		 
		try{  
			String jsonStr = new String(jsonData, charsetName);
			req = (JSONObject) JSON.parse(jsonStr);
		} catch (Exception e) {
			e.printStackTrace();
			error = e;
		}
		 
		if(error == null){
			try{ 
				module = req.getString("module");
				method = req.getString("method");
				args = req.getJSONArray("params");
				paramTypes = req.getJSONArray("paramTypes");
                if(paramTypes != null){
                	StringBuilder sb = new StringBuilder();
                	for(int i=0; i<paramTypes.size(); i++){
                		if (paramTypes.get(i) == null) continue;
                		sb.append(paramTypes.get(i).toString());
                	}
                	paramMD5 = sb.toString();
                }
			} catch (Exception e) {
				error = e;
			}
			if(module == null){
				module = "";
			}
			if(method == null){
				error = new IllegalArgumentException("missing method name");
			}
		}
		
		String key = module+":"+method+":"+paramMD5;
		if(error == null){
			if(this.methods.containsKey(key)){
				target = this.methods.get(key); 
			}else{ 
				String keyWithoutParams = module+":"+method;
				if(this.methods.containsKey(keyWithoutParams)){
					target = this.methods.get(keyWithoutParams); 
				} else {
					String msg = String.format("%s:%s not found, module may not set, or wrong", module, method);
					error = new IllegalArgumentException(msg);
				}
			}
		}
		Class<?> returnType=null;
		if(error == null){
			try { 
				Class<?>[] paramTypeList = target.method.getParameterTypes();
				returnType = target.method.getReturnType();
				
				//添加返回值类型，判断是否为数组
				if(returnType != null && returnType.isArray()){
					returnType = returnType.getComponentType(); 
				}
				
				if(args == null){ //FIX of none parameters
					args = new JSONArray();
				}
				
				if(paramTypeList.length == args.size()){
					Object[] params = new Object[paramTypeList.length]; 
					for(int i=0; i<paramTypeList.length; i++){
						if (paramTypeList[i].getName().equals("java.lang.Class"))
							params[i] = Thread.currentThread().getContextClassLoader().loadClass(args.get(i).toString());
						else 
							params[i] = args.getObject(i, paramTypeList[i]);
					}
					result = target.method.invoke(target.instance, params); 
				} else {
					String requiredParamTypeString = "";
					for(int i=0;i<paramTypeList.length;i++){
						Class<?> paramType = paramTypeList[i]; 
						requiredParamTypeString += paramType.getName();
						if(i<paramTypeList.length-1){
							requiredParamTypeString += ", ";
						}
					}
					String gotParamsString = "";
					for(int i=0;i<args.size();i++){ 
						gotParamsString += args.getString(i);
						if(i<args.size()-1){
							gotParamsString += ", ";
						}
					}
					String msg = String.format("Method:%s(%s), called with %s(%s)", 
							target.method.getName(), requiredParamTypeString, target.method.getName(),gotParamsString);
					error = new IllegalArgumentException(msg);
				} 
			} catch (Throwable e) {
			    log.error(e.getMessage(), e);
				error = e;
			}
		} 
		 
		if(error == null){    
			return okMessage(result, returnType, charsetName);
		} else { 
			String status = "500";
			if(error instanceof IllegalArgumentException){
				status = "400";
			}
			return errorMessage(error, charsetName, status);
		} 
	}

	private Message okMessage(Object result, Class<?> returnType, String encoding){
		Message res = new Message();
		JSONObject data = new JSONObject();
		res.setStatus("200");
		data.put("result", result); 
		if(returnType != null){
			data.put("returnType", returnType); 
		}
		
		byte[] resBytes = null;  
		resBytes = JsonCodec.toJSONBytes(data, encoding, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteClassName);
		res.setJsonBody(resBytes);
		return res;
	}
	
	private Message errorMessage(Throwable error, String encoding, String status){
		Message res = new Message();
		res.setStatus(status);
		
		JSONObject data = new JSONObject(); 
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		if(error.getCause() != null){
			error = error.getCause();
		}
		error.printStackTrace(pw); 
		data.put("error", error); 
		data.put("stack_trace", sw.toString()); 
		
		byte[] resBytes = null;  
		resBytes = JsonCodec.toJSONBytes(data, encoding, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteClassName);
		res.setJsonBody(resBytes);
		return res;
	}
	
	@Override
	public Message handleRequest(Message request) {  
		String encoding = request.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		} 
		try { 
			byte[] jsonBytes = request.getBody();  
			return this.handleJsonRequest(jsonBytes, encoding);
		} catch (Throwable error) {
			return errorMessage(error, encoding, "500");
		} 
	} 
}
