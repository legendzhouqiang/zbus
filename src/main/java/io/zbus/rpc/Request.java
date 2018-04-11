package io.zbus.rpc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Request extends HashMap<String, Object> {   
	private static final long serialVersionUID = -2112706466764692497L;   
	
	public String getMethod() {
		return get("method");
	} 
	public void setMethod(String value) {
		put("method", value);
	} 
	 
	public String getId() { 
		return get("id");
	}
	public void setId(String value) {
		put("id", value);
	} 
	
	public String getModule() {
		return get("module");
	} 
	public void setModule(String value) {
		put("module", value);
	} 
	
	public List<Object> getParams() {
		return get("params");
	} 
	public void setParams(Object... value) {
		put("params", Arrays.asList(value));
	} 
	public void setParams(List<Object> value) {
		put("params", value);
	} 
	
	public String[] getParamTypes() {
		return get("paramTypes");
	} 
	public void setParamTypes(String[] value) {
		put("paramTypes", value);
	}   
	public void setParamTypes(Class<?>[] value) {
		String[] paramTypes = new String[value.length];
		int i = 0;
		for(Class<?> type : value) {
			paramTypes[i++] = type.getName();
		}
		setParamTypes(paramTypes);
	}  
	
	public String getApiKey() { 
		return get("apiKey");
	}
	public void setApiKey(String value) {
		put("apiKey", value);
	} 
	
	public String getSignature() { 
		return get("signature");
	}
	public void setSignature(String value) {
		put("signature", value);
	} 
	
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T)super.get(key);
	}
}