package org.zbus.log.impl;

import org.zbus.log.Logger;
import org.zbus.log.LoggerFactory;

public class JdkLoggerFactory implements LoggerFactory {
	
	public Logger getLogger(Class<?> clazz) {
		return new JdkLogger(clazz);
	}
	
	public Logger getLogger(String name) {
		return new JdkLogger(name);
	}
}
