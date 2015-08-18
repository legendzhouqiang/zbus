
package org.zbus.log;

import org.zbus.log.impl.JdkLoggerFactory;

public abstract class Logger { 
	private static LoggerFactory factory;
	
	static {
		initDefaultFactory();
	}
	
	public static void setLoggerFactory(LoggerFactory factory) {
		if (factory != null) {
			Logger.factory = factory;
		}
	}
	
	public static Logger getLogger(Class<?> clazz) {
		return factory.getLogger(clazz);
	}
	
	public static Logger getLogger(String name) {
		return factory.getLogger(name);
	}
	
	public static void initDefaultFactory() {
		if (factory != null){
			return ;
		}
		String defaultFactory = String.format("%s.impl.Log4jLoggerFactory", Logger.class.getPackage().getName());
		try {
			//default to Log4j
			Class.forName("org.apache.log4j.Logger");
			Class<?> factoryClass = Class.forName(defaultFactory);
			factory = (LoggerFactory)factoryClass.newInstance();
		} catch (Exception e) { 
			factory = new JdkLoggerFactory();
		}
	}
	
	public void debug(String format, Object... args){
		debug(String.format(format, args));
	} 
	
	public void info(String format, Object... args){
		info(String.format(format, args));
	}
	
	public void warn(String format, Object... args){
		warn(String.format(format, args));
	}
	
	public void error(String format, Object... args){
		error(String.format(format, args));
	}
	
	public abstract void debug(String message);
	
	public abstract void debug(String message, Throwable t);
	
	public abstract void info(String message);
	
	public abstract void info(String message, Throwable t);
	
	public abstract void warn(String message);
	
	public abstract void warn(String message, Throwable t);
	
	public abstract void error(String message);
	
	public abstract void error(String message, Throwable t);
	
	public abstract void fatal(String message);
	
	public abstract void fatal(String message, Throwable t);
	
	public abstract boolean isDebugEnabled();

	public abstract boolean isInfoEnabled();

	public abstract boolean isWarnEnabled();

	public abstract boolean isErrorEnabled();
	
	public abstract boolean isFatalEnabled();
}

