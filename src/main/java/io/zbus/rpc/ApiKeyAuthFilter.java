package io.zbus.rpc;

import io.zbus.auth.ApiKeyProvider;
import io.zbus.auth.AuthResult;
import io.zbus.auth.DefaultAuth;
import io.zbus.auth.RequestAuth;

public class ApiKeyAuthFilter implements RpcFilter {
	private RequestAuth auth;
	
	public ApiKeyAuthFilter(ApiKeyProvider apiKeyProvider) {
		this.auth = new DefaultAuth(apiKeyProvider);
	} 
	
	public ApiKeyAuthFilter(RequestAuth auth) {
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
