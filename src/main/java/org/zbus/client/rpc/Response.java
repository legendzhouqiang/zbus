package org.zbus.client.rpc;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Response { 
	public static final String KEY_RESULT = "result";
	public static final String KEY_STACK_TRACE = "stackTrace"; 
	
	private Object result;  
	private Throwable error;
	private String stackTrace;
	private String encoding = "UTF-8";
	
	public Object getResult() {
		return result;
	}
	public void setResult(Object result) {
		this.result = result;
	}
	
	public Throwable getError() {
		return error;
	}
	
	public void setError(Throwable error) {
		this.error = error;
		if(error == null) return;
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		if(error.getCause() != null){
			error = error.getCause();
		}
		error.printStackTrace(pw);  
		this.stackTrace = sw.toString();
	}
	public String getStackTrace() {
		return stackTrace;
	}
	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}
	public String getEncoding() {
		return encoding;
	}
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
}
