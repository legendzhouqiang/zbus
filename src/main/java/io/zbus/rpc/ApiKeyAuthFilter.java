package io.zbus.rpc;

import io.zbus.auth.ApiAuth;
import io.zbus.auth.AuthResult;

public class ApiKeyAuthFilter implements RpcFilter {
	private ApiAuth auth;
	
	public ApiKeyAuthFilter(ApiAuth auth) {
		this.auth = auth;
	}
	
	@Override
	public boolean doFilter(Request request, Response response) { 
		AuthResult res = auth.auth(request);
		if(res.success) return true;
		
		response.setStatus(403);
		response.setData(res.message);
		
		return false;
	} 
}
