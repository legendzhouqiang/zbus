package org.zbus.mq.server.auth;

public interface Auth {
	boolean auth(String appid, String token);
	void setAccessToken(String accessToken);
}
