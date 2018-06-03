package io.zbus.rpc;

import java.util.Map;

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
	public boolean doFilter(Map<String, Object> request, Map<String, Object> response) { 
		AuthResult res = auth.auth(request);
		if(res.success) return true;
		
		response.put(Protocol.STATUS, 403);
		response.put(Protocol.BODY, res.message);
		
		return false;
	} 
}
