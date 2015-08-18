package org.zbus.net.core;


 

public interface Codec{ 
	/**
	 * 消息对象到网络字节码编码
	 * 
	 * @param msg
	 * @return
	 */
	IoBuffer encode(Object msg);
	/**
	 * 从网络字节码中解码消息对象
	 * @param buff
	 * @return
	 */
	Object decode(IoBuffer buff);
}
