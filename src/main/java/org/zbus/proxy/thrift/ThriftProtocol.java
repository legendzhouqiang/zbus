package org.zbus.proxy.thrift;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.zbus.net.core.IoBuffer;

public class ThriftProtocol { 
	private static final TStruct ANONYMOUS_STRUCT = new TStruct();
	protected static final int VERSION_MASK = 0xffff0000;
	protected static final int VERSION_1 = 0x80010000;

	protected IoBuffer ioBuffer;

	public ThriftProtocol(IoBuffer trans) {
		this.ioBuffer = trans;
	}

	public void writeMessageBegin(TMessage message) {
		int version = VERSION_1 | message.type;
		writeI32(version);
		writeString(message.name);
		writeI32(message.seqid);
	}

	public void writeMessageEnd() {
	}

	public void writeStructBegin(TStruct struct) {
	}

	public void writeStructEnd() {
	}

	public void writeFieldBegin(TField field) {
		writeByte(field.type);
		writeI16(field.id);
	}

	public void writeFieldEnd() {
	}

	public void writeFieldStop() {
		writeByte(TType.STOP);
	}

	public void writeBool(boolean b) {
		writeByte(b ? (byte) 1 : (byte) 0);
	}

	public void writeByte(byte b) {
		ioBuffer.writeByte(b);
	}

	public void writeI16(short i16) {
		ioBuffer.writeShort(i16);
	}

	public void writeI32(int i32) {
		ioBuffer.writeInt(i32);
	}

	public void writeI64(long i64) {
		ioBuffer.writeLong(i64);
	}

	public void writeDouble(double dub) {
		writeI64(Double.doubleToLongBits(dub));
	}

	public void writeString(String str) {
		try {
			byte[] dat = str.getBytes("UTF-8");
			writeI32(dat.length);
			ioBuffer.writeBytes(dat, 0, dat.length);
		} catch (UnsupportedEncodingException uex) {
			throw new RuntimeException("JVM DOES NOT SUPPORT UTF-8");
		}
	}

	public void writeBinary(ByteBuffer bin) {
		int length = bin.limit() - bin.position();
		writeI32(length);
		ioBuffer.writeBytes(bin.array(), bin.position() + bin.arrayOffset(),
				length);
	}

	public TMessage readMessageBegin() {
		int size = readI32();
		int version = size & VERSION_MASK;
		if (version != VERSION_1) {
			throw new RuntimeException("Bad version in readMessageBegin");
		}
		return new TMessage(readString(), (byte) (size & 0x000000ff), readI32());

	}

	public void readMessageEnd() {
	}

	public TStruct readStructBegin() {
		return ANONYMOUS_STRUCT;
	}

	public void readStructEnd() {
	}

	public TField readFieldBegin() {
		byte type = readByte();
		short id = type == TType.STOP ? 0 : readI16();
		return new TField("", type, id);
	}

	public void readFieldEnd() {
	}

	public boolean readBool() {
		return (readByte() == 1);
	}

	public byte readByte() {
		return ioBuffer.readByte();
	}

	public short readI16() {
		return ioBuffer.readShort();
	}

	public int readI32() {
		return ioBuffer.readInt();
	}

	public long readI64() {
		return ioBuffer.readLong();
	}

	public double readDouble() {
		return Double.longBitsToDouble(readI64());
	}

	public String readString() {
		int size = readI32();
		byte[] bb = new byte[size];
		ioBuffer.readBytes(bb);
		return new String(bb);
	}

	public static class TMessage {
		public TMessage() {
			this("", TType.STOP, 0);
		}

		public TMessage(String n, byte t, int s) {
			name = n;
			type = t;
			seqid = s;
		}

		public final String name;
		public final byte type;
		public final int seqid;
	}

	public static class TField {

		public TField() {
			this("", TType.STOP, (short) 0);
		}

		public TField(String n, byte t, short i) {
			name = n;
			type = t;
			id = i;
		}

		public final String name;
		public final byte type;
		public final short id;
	}

	public static class TStruct {
		public TStruct() {
			this("");
		}

		public TStruct(String n) {
			name = n;
		}

		public final String name;
	}

	public static class TMessageType {
		public static final byte CALL = 1;
		public static final byte REPLY = 2;
		public static final byte EXCEPTION = 3;
		public static final byte ONEWAY = 4;
	}

	public static class TType {
		public static final byte STOP = 0;
		public static final byte VOID = 1;
		public static final byte BOOL = 2;
		public static final byte BYTE = 3;
		public static final byte DOUBLE = 4;
		public static final byte I16 = 6;
		public static final byte I32 = 8;
		public static final byte I64 = 10;
		public static final byte STRING = 11;
		public static final byte STRUCT = 12; 
	} 
}
