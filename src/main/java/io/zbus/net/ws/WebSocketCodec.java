package io.zbus.net.ws;

import java.net.URI;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class WebSocketCodec extends MessageToMessageCodec<Object, byte[]> {
	private static final Logger log = LoggerFactory.getLogger(WebSocketCodec.class); 
	
	private WebSocketClientHandshaker handshaker; 
	private URI uri;

	public WebSocketCodec(URI uri) {
		this.uri = uri;
	} 
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception { 
		this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
		
		handshaker.handshake(ctx.channel());  
		
		super.channelActive(ctx);
	}  
	
	@Override
	protected void encode(ChannelHandlerContext ctx, byte[] msg, List<Object> out) throws Exception { 
		WebSocketFrame frame = new TextWebSocketFrame(new String(msg)); //FIXME binary frame?
		out.add(frame);
		return; 
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, Object obj, List<Object> out) throws Exception { 
		if (obj instanceof WebSocketFrame) {
			byte[] msg = decodeWebSocketFrame(ctx, (WebSocketFrame) obj);
			if (msg != null) {
				out.add(msg);
			}
			return;
		} 
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Channel ch = ctx.channel();
		if (!handshaker.isHandshakeComplete()) {
			try {
				handshaker.finishHandshake(ch, (FullHttpResponse) msg);  
			} catch (WebSocketHandshakeException e) { 
				log.error(e.getMessage(), e); 
			}
			return;
		}

		super.channelRead(ctx, msg);
	} 
	

	private byte[] decodeWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return null;
		}

		if (frame instanceof PingWebSocketFrame) {
			ctx.write(new PongWebSocketFrame(frame.content().retain()));
			return null;
		}

		if (frame instanceof TextWebSocketFrame) {
			TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
			return parseMessage(textFrame.content());
		}

		if (frame instanceof BinaryWebSocketFrame) {
			BinaryWebSocketFrame binFrame = (BinaryWebSocketFrame) frame;
			return parseMessage(binFrame.content());
		}

		log.warn("Message format error: " + frame);
		return null;
	}

	private byte[] parseMessage(ByteBuf buf) {
		int size = buf.readableBytes();
		byte[] data = new byte[size];
		buf.readBytes(data);
		return data;
	} 
}
