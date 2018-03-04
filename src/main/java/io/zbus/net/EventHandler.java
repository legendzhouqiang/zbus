package io.zbus.net;

import java.io.IOException;

public interface EventHandler { 
	void handle() throws IOException;   
}