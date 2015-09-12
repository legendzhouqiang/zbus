package org.zbus.proxy.thrift;
   
import org.zbus.mq.Protocol;
import org.zbus.net.core.Codec;
import org.zbus.net.core.IoBuffer;
import org.zbus.net.http.Message;
import org.zbus.proxy.thrift.ThriftProtocol.TField;
import org.zbus.proxy.thrift.ThriftProtocol.TMessage;
import org.zbus.proxy.thrift.ThriftProtocol.TMessageType;
import org.zbus.proxy.thrift.ThriftProtocol.TStruct;
import org.zbus.proxy.thrift.ThriftProtocol.TType;

public class ThriftCodec implements Codec {
	@Override
	public IoBuffer encode(Object obj) {
		// Message ==> Thrift Binary
		Message msg = (Message) obj;
		IoBuffer thriftBody = IoBuffer.allocate(1024 * 4);

		ThriftProtocol proto = new ThriftProtocol(thriftBody);

		int seqId = Integer.valueOf(msg.getId());
		TMessage tmsg = new TMessage("rpc", TMessageType.REPLY, seqId);
		proto.writeMessageBegin(tmsg);
		TStruct struct = new TStruct();
		proto.writeStructBegin(struct);
		TField field = new TField("result", TType.STRING, (byte) 0);
		proto.writeFieldBegin(field);
		proto.writeString(msg.getBodyString());
		proto.writeFieldStop();
		proto.writeStructEnd();
		proto.writeMessageEnd();

		
		thriftBody.flip();
		IoBuffer buff = IoBuffer.allocate(4 + thriftBody.remaining());
		buff.writeInt(thriftBody.remaining());
		buff.writeBytes(thriftBody.array(), thriftBody.position(),
				thriftBody.remaining());

		buff.flip();
		return buff;
	}

	@Override
	public Object decode(IoBuffer buff) {
		// Thrift binary ==> Message
		if (buff.remaining() < 4) {
			return null;
		}
		buff.mark();
		int size = buff.readInt();
		if (buff.remaining() < size) {
			buff.reset();
			return null;
		}

		ThriftProtocol proto = new ThriftProtocol(buff);

		Message zbusMsg = new Message();

		TMessage msg = proto.readMessageBegin();
		if ("rpc".equals(msg.name)) {
			zbusMsg.setCmd(Protocol.Produce);
			zbusMsg.setAck(false);
		} else {
			zbusMsg.setCmd(msg.name);
		}
		zbusMsg.setId(msg.seqid);

		proto.readStructBegin();// struct begin
		proto.readFieldBegin();
		String mq = proto.readString();
		proto.readFieldEnd();
		zbusMsg.setMq(mq);

		proto.readFieldBegin();
		String body = proto.readString();
		proto.readFieldEnd();
		zbusMsg.setBody(body);
		proto.readFieldBegin(); // consume stop

		proto.readStructEnd();// struct end
		proto.readMessageEnd();
		return zbusMsg;
	}

}
