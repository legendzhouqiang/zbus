package org.zbus.log.impl;

import org.apache.log4j.Level;
import org.zbus.log.Logger;

public class Log4jLogger extends Logger { 
	private org.apache.log4j.Logger log;
	
	private static final String callerFQCN = Log4jLogger.class.getName();
	
	Log4jLogger(Class<?> clazz) {
		log = org.apache.log4j.Logger.getLogger(clazz);
	}
	
	Log4jLogger(String name) {
		log = org.apache.log4j.Logger.getLogger(name);
	}
	
	public void debug(String format, Object... args){
		String msg = String.format(format, args);
		log.log(callerFQCN, Level.DEBUG, msg, null);
	} 
	
	public void info(String format, Object... args){
		String msg = String.format(format, args);
		log.log(callerFQCN, Level.INFO, msg, null);
	}
	
	public void warn(String format, Object... args){
		String msg = String.format(format, args);
		log.log(callerFQCN, Level.WARN, msg, null);
	}
	
	public void error(String format, Object... args){
		String msg = String.format(format, args);
		log.log(callerFQCN, Level.ERROR, msg, null);
	}
	
	
	public void info(String message) {
		log.log(callerFQCN, Level.INFO, message, null);
	}
	
	public void info(String message, Throwable t) {
		log.log(callerFQCN, Level.INFO, message, t);
	}
	
	public void debug(String message) {
		log.log(callerFQCN, Level.DEBUG, message, null);
	}
	
	public void debug(String message, Throwable t) {
		log.log(callerFQCN, Level.DEBUG, message, t);
	}
	
	public void warn(String message) {
		log.log(callerFQCN, Level.WARN, message, null);
	}
	
	public void warn(String message, Throwable t) {
		log.log(callerFQCN, Level.WARN, message, t);
	}
	
	public void error(String message) {
		log.log(callerFQCN, Level.ERROR, message, null);
	}
	
	public void error(String message, Throwable t) {
		log.log(callerFQCN, Level.ERROR, message, t);
	}
	
	public void fatal(String message) {
		log.log(callerFQCN, Level.FATAL, message, null);
	}
	
	public void fatal(String message, Throwable t) {
		log.log(callerFQCN, Level.FATAL, message, t);
	}
	
	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}
	
	public boolean isInfoEnabled() {
		return log.isInfoEnabled();
	}
	
	public boolean isWarnEnabled() {
		return log.isEnabledFor(Level.WARN);
	}
	
	public boolean isErrorEnabled() {
		return log.isEnabledFor(Level.ERROR);
	}
	
	public boolean isFatalEnabled() {
		return log.isEnabledFor(Level.FATAL);
	}
}

