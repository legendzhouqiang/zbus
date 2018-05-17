package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

public class GenericService implements GenericInvocation {

	@Override
	public Object invoke(String funcName, Map<String, Object> params) {
		Map<String, Object> res = new HashMap<>();
		res.put("invokedFunc", funcName);
		res.put("invokedParams", params);
		return res;
	} 
}