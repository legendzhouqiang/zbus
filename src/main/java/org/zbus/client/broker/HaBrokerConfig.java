package org.zbus.client.broker;


public class HaBrokerConfig extends SingleBrokerConfig { 
	private String trackAddrList = "127.0.0.1:16666;127.0.0.1:16667";

	public String getTrackAddrList() {
		return trackAddrList;
	}
	public void setTrackAddrList(String trackAddrList) {
		this.trackAddrList = trackAddrList;
	}	
	
	@Override
	public HaBrokerConfig clone() {
		HaBrokerConfig res = (HaBrokerConfig)super.clone();
		res.trackAddrList = trackAddrList;
		return res;
	}
}
