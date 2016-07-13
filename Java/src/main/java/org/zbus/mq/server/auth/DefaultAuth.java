package org.zbus.mq.server.auth;

public class DefaultAuth implements Auth{
	private String accessToken = "";
	public DefaultAuth(String accessToken){
		if(accessToken == null){
			accessToken = "";
		}
		this.accessToken = accessToken;
	}
	@Override
	public boolean auth(String appid, String token) {
		if(token == null) token = ""; //null assumed to be empty
		return accessToken.equals(token);
	}
	public void setAccessToken(String accessToken) {
		if(accessToken == null){ //null assumed to be empty
			accessToken = "";
		}
		this.accessToken = accessToken;
	} 
}