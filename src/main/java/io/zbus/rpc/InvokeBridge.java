package io.zbus.rpc;

import java.util.Map;

public interface InvokeBridge {
	public Object invoke(String funcName, Map<String, Object> params); 
}
	