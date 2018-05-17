package io.zbus.rpc;

import java.util.Map;

public interface GenericInvocation {
	public Object invoke(String funcName, Map<String, Object> params); 
}
	