package io.zbus.rpc;

import java.util.Map;

public class Request {  
	public String id;          //[O]Request ID, for match
	public String module;      //[O]
	public String method;      //[R]
	public Object[] params;    //[O]  
	public String[] paramTypes;//[O] method override support    
	
	public Map<String, Object> attachment;
}