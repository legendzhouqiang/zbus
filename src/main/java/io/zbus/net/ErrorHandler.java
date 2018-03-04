package io.zbus.net;

import java.io.IOException;

public interface ErrorHandler { 
	void handle(Throwable e) throws IOException;   
}