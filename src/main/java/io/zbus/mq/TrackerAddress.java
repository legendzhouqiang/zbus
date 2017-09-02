package io.zbus.mq;

import io.zbus.transport.ServerAddress;

public class TrackerAddress extends ServerAddress {
	public String token;
	public String sslCertFile;
	
	public TrackerAddress(){
		
	}
	
	public TrackerAddress(String address) {
		super(address);
	}
	
	public TrackerAddress(String address, boolean sslEnabled) {
		super(address, sslEnabled);
	}
	
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getSslCertFile() {
		return sslCertFile;
	}
	public void setSslCertFile(String sslCertFile) {
		this.sslCertFile = sslCertFile;
	} 
}
