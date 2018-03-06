
package io.zbus.net.http;


public class HttpException extends RuntimeException {  
	private static final long serialVersionUID = 8445590420018236422L;
	
	private Integer code;  
	private String request;
	
	public HttpException(String message) {
		super(message); 
	}

	public HttpException() { 
	}

	public HttpException(String message, Throwable cause) {
		super(message, cause); 
	}

	public HttpException(Throwable cause) {
		super(cause); 
	}
	
	public HttpException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace); 
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}  
	
}