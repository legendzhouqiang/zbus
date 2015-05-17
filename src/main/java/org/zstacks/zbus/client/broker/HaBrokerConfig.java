package org.zstacks.zbus.client.broker;


public class HaBrokerConfig extends SingleBrokerConfig { 
	private String trackAddrList = "127.0.0.1:16666;127.0.0.1:16667";

	public String getTrackAddrList() {
		return trackAddrList;
	}
	public void setTrackAddrList(String trackAddrList) {
		this.trackAddrList = trackAddrList;
	}	
	
	
	/**
	 * 对HA模式，brokerAddress不起作用, 使用trackAddrList
	 */
	@Override
	@Deprecated
	public void setBrokerAddress(String brokerAddress) { 
		super.setBrokerAddress(brokerAddress);
	}
	
	@Override
	public HaBrokerConfig clone() {
		HaBrokerConfig res = (HaBrokerConfig)super.clone();
		res.trackAddrList = trackAddrList;
		return res;
	}
}
