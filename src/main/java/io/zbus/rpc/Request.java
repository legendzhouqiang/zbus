package io.zbus.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request { 
	public String id;
	public String command;   
	public List<Object> params = new ArrayList<>();    
	
	public Map<String, Object> properties = new HashMap<>();   
	
	
	public static final String MODULE = "module"; 
	public static final String PARAM_TYPES = "paramTypes"; 
}