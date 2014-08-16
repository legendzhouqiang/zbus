package org.zbus.remoting.znet; 

public interface Codec{
	public IoBuffer encode(Object msg);
	public Object decode(IoBuffer buff);
}
