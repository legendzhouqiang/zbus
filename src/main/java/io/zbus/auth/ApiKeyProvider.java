package io.zbus.auth;

public interface ApiKeyProvider { 
	String secretKey(String apiKey);
	boolean apiKeyExists(String apiKey);
}
