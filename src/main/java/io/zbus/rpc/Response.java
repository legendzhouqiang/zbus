package io.zbus.rpc;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Response { 
	public static final String KEY_RESULT = "result";
	public static final String KEY_STACK_TRACE = "stackTrace";
	public static final String KEY_ERROR = "error";
	
	private String stackTrace; //null if no error, otherwise populated with stack trace message
	private Object result;     //could be null if void method invoked  
	private Throwable error;   //could be null if error can not be deserialized 
	
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

	@Override
	public String toString() {
		return "Response [stackTrace=" + stackTrace + ", result=" + result + ", error=" + error + "]";
	}  
}