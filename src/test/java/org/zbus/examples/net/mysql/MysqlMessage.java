package org.zbus.examples.net.mysql;

import org.zbus.net.Client.MsgHandler;
import org.zbus.net.Invoker;
import org.zbus.net.core.IoBuffer;

public class MysqlMessage {
	public int length;
	public byte pakgId;
	public byte[] data;
	public static interface MysqlMessageHandler extends MsgHandler<MysqlMessage> { }
	public static interface MysqlMessageInvoker extends Invoker<MysqlMessage, MysqlMessage> { }	
	public static interface MysqlMessageProcessor { 
		MysqlMessage process(MysqlMessage request);
	}
	
	public byte[] toBytes(){
		IoBuffer bb = toIoBuffer(); 
		byte[] b = new byte[bb.remaining()];
		bb.readBytes(b);
		return b;
	}
	
	public IoBuffer toIoBuffer(){
		IoBuffer bb =   IoBuffer.allocate(data.length+3+1);
		if(data != null){
			int n = data.length;
			bb.writeByte((byte) (n & 0xff));
			bb.writeByte( (byte) (n >> 8 & 0xff));
			bb.writeByte( (byte) (n >> 16 & 0xff));
			bb.writeByte(pakgId);
			bb.writeBytes(data);
		}
		
		
		return bb;
	}
}
