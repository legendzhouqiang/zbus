package io.zbus.rpc;

public interface RpcFilter { 
	/**
	 * 
	 * @param request
	 * @param response
	 * @return true if continue to handle request response
	 */
	boolean doFilter(Request request, Response response);
}
