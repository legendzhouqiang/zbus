package io.zbus.transport;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface Invoker {
	
	void invoke(Map<String, Object> req, DataHandler<Map<String, Object>> dataHandler);
	
	void invoke(Map<String, Object> req, DataHandler<Map<String, Object>> dataHandler, ErrorHandler errorHandler);
	
	Map<String, Object> invoke(Map<String, Object> req) throws IOException, InterruptedException;
	
	Map<String, Object> invoke(Map<String, Object> req, long timeout, TimeUnit timeUnit) throws IOException, InterruptedException;
}
