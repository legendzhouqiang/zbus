package io.zbus.net;

import java.io.IOException;

public interface ErrorHandler { 
	void onError(Throwable e, Session session) throws IOException;   
}