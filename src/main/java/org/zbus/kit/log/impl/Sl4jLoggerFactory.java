/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.kit.log.impl;
 

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;

public class Sl4jLoggerFactory implements LoggerFactory {
	
	public Logger getLogger(Class<?> clazz) {
		return new Sl4jLogger(clazz);
	}
	
	public Logger getLogger(String name) {
		return new Sl4jLogger(name);
	}
}
class Sl4jLogger extends Logger { 
	private org.slf4j.Logger log; 
	
	private static final Marker maker = MarkerFactory.getMarker(Log4jLogger.class.getName());
	
	Sl4jLogger(Class<?> clazz) { 
		log = org.slf4j.LoggerFactory.getLogger(clazz);
	}
	
	Sl4jLogger(String name) {
		log = org.slf4j.LoggerFactory.getLogger(name);
	}
	
	public void debug(String format, Object... args){
		String msg = String.format(format, args); 
		log.debug(maker, msg);
	} 
	
	public void info(String format, Object... args){
		String msg = String.format(format, args);
		log.info(maker, msg);
	}
	
	public void warn(String format, Object... args){ 
		String msg = String.format(format, args);
		log.warn(maker, msg);
	}
	
	public void error(String format, Object... args){ 
		String msg = String.format(format, args);
		log.error(maker, msg);
	}
	
	
	public void info(String message) {
		log.info(message);
	}
	
	public void info(String message, Throwable t) {
		log.info(message, t);
	}
	
	public void debug(String message) {
		log.debug(message);
	}
	
	public void debug(String message, Throwable t) {
		log.debug(message, t);
	}
	
	public void warn(String message) {
		log.warn(message);
	}
	
	public void warn(String message, Throwable t) {
		log.warn(message, t);
	}
	
	public void error(String message) {
		log.error(message);
	}
	
	public void error(String message, Throwable t) {
		log.error(message, t);
	}
	
	public void fatal(String message) {
		log.error(message);
	}
	
	public void fatal(String message, Throwable t) {
		log.error(message, t);
	}
	
	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}
	
	public boolean isInfoEnabled() {
		return log.isInfoEnabled();
	}
	
	public boolean isWarnEnabled() {
		return log.isWarnEnabled();
	}
	
	public boolean isErrorEnabled() {
		return log.isErrorEnabled();
	}
	
	public boolean isFatalEnabled() {
		return log.isErrorEnabled();
	}
}