package io.zbus.rpc;

import java.util.Map;

public interface RpcFilter { 
	/**
	 * 
	 * @param request
	 * @param response
	 * @return true if continue to handle request response
	 */
	boolean doFilter(Map<String, Object> request, Map<String, Object> response);
}
