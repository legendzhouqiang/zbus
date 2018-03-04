package io.zbus.net.ws;

import java.nio.ByteBuffer;

public interface WebSocket {
	void connect();
	void onopen();
	void onclose();
	void onmessage();
	void onerror();
	void send(ByteBuffer data);
}
