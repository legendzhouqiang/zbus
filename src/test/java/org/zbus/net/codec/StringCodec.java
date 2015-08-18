package org.zbus.net.codec;

import org.zbus.net.core.Codec;
import org.zbus.net.core.IoBuffer;

public class StringCodec implements Codec{ 
	
	public IoBuffer encode(Object obj) { 
		if(!(obj instanceof String)){ 
			throw new RuntimeException("Message unknown"); 
		} 
		
		String msg = (String)obj; 
		byte[] b = msg.getBytes();
		IoBuffer buf = IoBuffer.allocate(4 + b.length);
		buf.writeInt(b.length);
		buf.writeBytes(b);
		
		buf.flip();
		return buf; 
	}
        
	public Object decode(IoBuffer buf) {  
		if(buf.remaining() < 4) return null;
		buf.mark();
		int len = buf.readInt();
		if(buf.remaining() < len){
			buf.reset();
			return null;
		}
		byte[] b = new byte[len];
		buf.readBytes(b);
		return new String(b);
	}  
}