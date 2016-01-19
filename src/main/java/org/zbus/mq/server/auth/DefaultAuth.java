package org.zbus.mq.server.auth;

public class DefaultAuth implements Auth{
	private String accessToken;
	public DefaultAuth(String accessToken){
		this.accessToken = accessToken;
	}
	@Override
	public boolean auth(String appid, String token) {
		return accessToken.equals(token);
	}
}