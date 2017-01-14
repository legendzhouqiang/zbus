package io.zbus.net;

public interface Identifier<MSG>{ 
	void setId(MSG msg, String id);  
	String getId(MSG res); 
}
