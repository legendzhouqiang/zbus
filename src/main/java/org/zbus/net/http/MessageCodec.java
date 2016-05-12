package org.zbus.net.http;

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