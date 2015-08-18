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
package org.zbus.rpc;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * RPC响应，结果或者异常信息（堆栈）
 * @author 洪磊明(rushmore)
 *
 */
public class Response { 
	public static final String KEY_RESULT = "result";
	public static final String KEY_STACK_TRACE = "stackTrace"; 
	
	private Object result;  
	private Throwable error;
	private String stackTrace; //异常时候一定保证stackTrace设定，判断的逻辑以此为依据
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
