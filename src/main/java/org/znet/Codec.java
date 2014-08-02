package org.znet; 

public interface Codec{
	public IoBuffer encode(Object msg);
	public Object decode(IoBuffer buff);
}
