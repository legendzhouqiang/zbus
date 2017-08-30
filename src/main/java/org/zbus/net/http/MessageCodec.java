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
package org.zbus.net.http;

import org.zbus.net.core.Codec;
import org.zbus.net.core.IoBuffer;

public class MessageCodec implements Codec{  
	
	public IoBuffer encode(Object obj) { 
		if(!(obj instanceof Message)){ 
			throw new RuntimeException("Message unknown"); 
		}  
		
		Message msg = (Message)obj;   
		IoBuffer buf = msg.toIoBuffer(); 
		return buf; 
	}
        
	public Object decode(IoBuffer buf) {  
		int headerIdx = findHeaderEnd(buf);
		if(headerIdx == -1) return null; 
		
		int headerLen = headerIdx+1-buf.position();
		
		buf.mark();
		Message msg = new Message();  
		msg.decodeHeaders(buf.array(), buf.position(), headerLen);
		buf.position(buf.position()+headerLen);
		
		String contentLength = msg.getHead(Message.CONTENT_LENGTH);
		if(contentLength == null){ //just head 
			return msg;
		}
		
		int bodyLen = Integer.valueOf(contentLength); 
		if(buf.remaining()<bodyLen) {
			buf.reset();
			return null;
		}
		 
		byte[] body = new byte[bodyLen];
		buf.readBytes(body);
		msg.setBody(body); 
		
		return msg;
	} 
	
	private static int findHeaderEnd(IoBuffer buf){
		byte[] data = buf.array();
		int i = buf.position();
		int limit = buf.limit();
		while(i+3<limit){
			if(data[i] != '\r') {
				i += 1;
				continue;
			}
			if(data[i+1] != '\n'){
				i += 1;
				continue;
			}
			
			if(data[i+2] != '\r'){
				i += 3;
				continue;
			}
			
			if(data[i+3] != '\n'){
				i += 3;
				continue;
			}
			
			return i+3; 
		}
		return -1;
	}
}