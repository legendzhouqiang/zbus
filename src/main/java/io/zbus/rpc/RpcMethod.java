package io.zbus.rpc;

import java.util.ArrayList;
import java.util.List;

public class RpcMethod {
	public String module;
	public String method;
	public List<String> paramTypes = new ArrayList<>();
	public List<String> paramNames = new ArrayList<>();
	public String returnType; 
	public boolean authRequired;
}