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
package org.zbus.examples.net.mysql;

import org.zbus.net.core.Codec;
import org.zbus.net.core.IoBuffer;

public class MysqlMessageCodec implements Codec {
	
	@Override
	public IoBuffer encode(Object obj) {
		if (!(obj instanceof MysqlMessage)) {
			throw new RuntimeException("Message unknown");
		}
		MysqlMessage msg = (MysqlMessage) obj;
		IoBuffer buf = msg.toIoBuffer();
		
		buf.flip(); //TODO

		return buf;
	}

	public static final int readUB3(byte[] b) {
		try {
			int i = b[0] & 0xff;
			i |= (b[1] & 0xff) << 8;
			i |= (b[2] & 0xff) << 16;
			return i;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return 0;

	}

	@Override
	public Object decode(IoBuffer buf) {
		int headerLen = 3;
		
		if(buf.remaining() < headerLen) return null;
		
		buf.mark();
		MysqlMessage msg = new MysqlMessage();

		byte[] head = new byte[headerLen]; 
		buf.readBytes(head);

		msg.pakgId = buf.readByte();

		//buf.position(buf.position() + headerLen + 1); TODO WRONG, automatically forward

		int bodyLen = readUB3(head);
		if (buf.remaining() < bodyLen || bodyLen == 0) {
			buf.reset();
			return null;
		}

		byte[] body = new byte[bodyLen];
		buf.readBytes(body);
		msg.data = body;

		return msg;
	}

}