package io.zbus.net.ws;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.net.http.HttpMsg;

public class WebsocketCodec extends MessageToMessageCodec<Object, HttpMsg> {
	private static final Logger log = LoggerFactory.getLogger(WebsocketCodec.class); 
	
	private WebSocketClientHandshaker handshaker;
	private ChannelPromise handshakeFuture;

	public WebsocketCodec(WebSocketClientHandshaker handshaker) {
		this.handshaker = handshaker;
	} 
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		handshakeFuture = ctx.newPromise();
		handshaker.handshake(ctx.channel());
		super.channelActive(ctx);
	} 
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception { 
		super.channelInactive(ctx);
	}
	
	@Override
	protected void encode(ChannelHandlerContext ctx, HttpMsg msg, List<Object> out) throws Exception {
		// 1) WebSocket mode
		if (handshaker != null) {// websocket step in, Message To WebSocketFrame
			ByteBuf buf = Unpooled.wrappedBuffer(msg.toBytes());
			WebSocketFrame frame = new TextWebSocketFrame(buf);
			out.add(frame);
			return;
		}

		// 2) HTTP mode
		FullHttpMessage httpMsg = null;
		if (msg.getStatus() == null) {// as request
			httpMsg = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(msg.getMethod()),
					msg.getUrl());
		} else {// as response
			httpMsg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
					HttpResponseStatus.valueOf(Integer.valueOf(msg.getStatus())));
		}
		// content-type and encoding
		String contentType = msg.getHeader(HttpMsg.CONTENT_TYPE);
		String encoding = msg.getHeader(HttpMsg.ENCODING);
		if (encoding != null) {
			encoding = "utf-8";
			if (contentType == null) {
				contentType = "text/plain";
			}
			contentType += "; charset=" + encoding;
		}
		if (contentType != null) {
			httpMsg.headers().set(HttpMsg.CONTENT_TYPE, contentType);
		}

		for (Entry<String, String> e : msg.getHeaders().entrySet()) {
			if (e.getKey().equalsIgnoreCase(HttpMsg.CONTENT_TYPE))
				continue;
			if (e.getKey().equalsIgnoreCase(HttpMsg.ENCODING))
				continue;

			httpMsg.headers().add(e.getKey().toLowerCase(), e.getValue());
		}
		if (msg.getBody() != null) {
			httpMsg.content().writeBytes(msg.getBody());
		}

		out.add(httpMsg);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, Object obj, List<Object> out) throws Exception {
		// 1) WebSocket mode
		if (obj instanceof WebSocketFrame) {
			byte[] msg = decodeWebSocketFrame(ctx, (WebSocketFrame) obj);
			if (msg != null) {
				out.add(msg);
			}
			return;
		}

		// 2) HTTP mode
		if (!(obj instanceof HttpMessage)) {
			throw new IllegalArgumentException("HttpMessage object required: " + obj);
		}

		HttpMessage httpMsg = (HttpMessage) obj;
		HttpMsg msg = decodeHeaders(httpMsg);

		// Body
		ByteBuf body = null;
		if (httpMsg instanceof FullHttpMessage) {
			FullHttpMessage fullReq = (FullHttpMessage) httpMsg;
			body = fullReq.content();
		}

		if (body != null) {
			int size = body.readableBytes();
			if (size > 0) {
				byte[] data = new byte[size];
				body.readBytes(data);
				msg.setBody(data);
			}
		}

		out.add(msg);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Channel ch = ctx.channel();
		if (!handshaker.isHandshakeComplete()) {
			try {
				handshaker.finishHandshake(ch, (FullHttpResponse) msg); 
				handshakeFuture.setSuccess(); 
			} catch (WebSocketHandshakeException e) { 
				log.error(e.getMessage(), e);
				handshakeFuture.setFailure(e);
			}
			return;
		}

		super.channelRead(ctx, msg);
	}

	private HttpMsg decodeHeaders(HttpMessage httpMsg) {
		HttpMsg msg = new HttpMsg();
		Iterator<Entry<String, String>> iter = httpMsg.headers().iterator();
		while (iter.hasNext()) {
			Entry<String, String> e = iter.next();
			if (e.getKey().equalsIgnoreCase(HttpMsg.CONTENT_TYPE)) { // encoding
																		// and
																		// type
				String[] typeInfo = httpContentType(e.getValue());
				msg.setHeader(HttpMsg.CONTENT_TYPE, typeInfo[0]);
				if (msg.getHeader(HttpMsg.ENCODING) == null) {
					msg.setHeader(HttpMsg.ENCODING, typeInfo[1]);
				}
			} else {
				msg.setHeader(e.getKey().toLowerCase(), e.getValue());
			}
		}

		if (httpMsg instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) httpMsg;
			msg.setMethod(req.getMethod().name());
			msg.setUrl(req.getUri());
		} else if (httpMsg instanceof HttpResponse) {
			HttpResponse resp = (HttpResponse) httpMsg;
			int status = resp.getStatus().code();
			msg.setStatus(status);
		}
		return msg;
	}

	private static String[] httpContentType(String value) {
		String type = "text/plain", charset = "utf-8";
		String[] bb = value.split(";");
		if (bb.length > 0) {
			type = bb[0].trim();
		}
		if (bb.length > 1) {
			String[] bb2 = bb[1].trim().split("=");
			if (bb2[0].trim().equalsIgnoreCase("charset")) {
				charset = bb2[1].trim();
			}
		}
		return new String[] { type, charset };
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

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { 
		if (!handshakeFuture.isDone()) {
			handshakeFuture.setFailure(cause);
		}
		ctx.close();
	}
}
