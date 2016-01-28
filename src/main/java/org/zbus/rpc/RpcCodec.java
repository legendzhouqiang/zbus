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

import org.zbus.net.http.Message;


public interface RpcCodec {
	Message  encodeRequest(Request request); 
	Message  encodeResponse(Response response); 
	Request  decodeRequest(Message msg); 
	Response decodeResponse(Message msg);
	/**
	 * 强制转换类型，比如JsonCodec中将JSON格式的对象转换为强类型
	 * 这个过程在方法本地调用之前组装参数（强类型匹配）的时候使用
	 * 
	 * @param param 弱类型（JSON/XML化的内存对象），简单类型也支持
	 * @param targetType 目标类型
	 * @return
	 * @throws ClassNotFoundException
	 */
	Object convert(Object param, Class<?> targetType) throws ClassNotFoundException;


	public static class Request{ 
		private String module = ""; //模块标识
		private String method;      //远程方法
		private Object[] params;    //参数列表
		private String[] paramTypes;
		private String encoding = "UTF-8";
		
		public String getModule() {
			return module;
		}
		public void setModule(String module) {
			this.module = module;
		}
		public String getMethod() {
			return method;
		}
		public void setMethod(String method) {
			this.method = method;
		}
		public Object[] getParams() {
			return params;
		}
		public void setParams(Object[] params) {
			this.params = params;
		}
		public String[] getParamTypes() {
			return paramTypes;
		}
		public void setParamTypes(String[] paramTypes) {
			this.paramTypes = paramTypes;
		}
		public String getEncoding() {
			return encoding;
		}
		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}

		public Request method(String method){
			this.method = method;
			return this;
		}
		public Request module(String module){
			this.module = module;
			return this;
		}
		public Request params(Object... params){
			this.params = params;
			return this;
		}
		public Request encoding(String encoding){
			this.encoding = encoding;
			return this;
		}
		public Request paramTypes(Class<?>... types){
			if(types == null) return this;
			this.paramTypes = new String[types.length];
			for(int i=0; i<types.length; i++){
				this.paramTypes[i]= types[i].getCanonicalName(); 
			}
			return this;
		}
		
		
		public static void normalize(Request req){
			if(req.module == null){
				req.module = "";
			}
			if(req.params == null){
				req.params = new Object[0];
			}
		}  
	}
	
	public static class Response { 
		public static final String KEY_RESULT = "result";
		public static final String KEY_STACK_TRACE = "stackTrace";
		public static final String KEY_ERROR = "error";
		
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
}
