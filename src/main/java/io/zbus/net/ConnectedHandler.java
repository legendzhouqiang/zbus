package io.zbus.net;

import java.io.IOException;

public interface ConnectedHandler { 
	void onConnected() throws IOException;   
}