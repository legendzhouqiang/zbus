package io.zbus.rpc;

import io.zbus.auth.ApiKeyProvider;
import io.zbus.auth.AuthResult;
import io.zbus.auth.DefaultAuth;
import io.zbus.auth.RequestAuth;

public class RpcAuthFilter implements RpcFilter {
	private RequestAuth auth;
	
	public RpcAuthFilter(ApiKeyProvider apiKeyProvider) {
		this.auth = new DefaultAuth(apiKeyProvider);
	} 
	
	public RpcAuthFilter(RequestAuth auth) {
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
