package io.zbus.rpc;

import java.util.ArrayList;
import java.util.List;
 

public class RpcMethod {
	private List<String> modules = new ArrayList<String>();
	private String name;
	private List<String> paramTypes = new ArrayList<String>();
	private String returnType;
	
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
