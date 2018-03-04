package io.zbus.net;

import java.io.IOException;

public interface DisconnectedHandler { 
	void onDisconnected() throws IOException;   
}