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
package io.zbus.rpc;

import io.zbus.mq.Message;


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
}
