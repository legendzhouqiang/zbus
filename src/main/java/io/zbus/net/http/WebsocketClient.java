package io.zbus.net.http;

import java.nio.charset.Charset;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
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
import io.zbus.net.Client;
import io.zbus.net.EventLoop;

public class WebsocketClient extends Client<String, String> {
	private static final Logger log = LoggerFactory.getLogger(WebsocketClient.class);
	private boolean websocketReady = false;
	
	private Charset charset = Charset.forName("UTF-8");
	
	public WebsocketClient(String address, final EventLoop loop) {
		super(address, loop); 
		triggerOpenWhenConnected = false;  //wait for handshake finished 
		codec(p -> { 
			p.add(new HttpRequestEncoder());
			p.add(new HttpResponseDecoder());
			p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
			p.add(new WebSocketCodec());
		}); 
	}
	
	@Override
	public boolean active() { 
		return super.active() && websocketReady;
	}
	
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
	class WebSocketCodec extends MessageToMessageCodec<Object,String> {  
		private WebSocketClientHandshaker handshaker;  
		
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception { 
			this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(
	                uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
			
			handshaker.handshake(ctx.channel());  
			
			super.channelActive(ctx);
		}  
		
		@Override
		protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception { 
			WebSocketFrame frame = new TextWebSocketFrame(msg); 
			out.add(frame);
			return; 
		}

		@Override
		protected void decode(ChannelHandlerContext ctx, Object obj, List<Object> out) throws Exception { 
			if (obj instanceof WebSocketFrame) {
				String msg = decodeWebSocketFrame(ctx, (WebSocketFrame) obj);
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
					websocketReady = true; 
					for(String req : cachedMessages) {
						sendMessage(req);
					}
					cachedMessages.clear();
					if(onOpen != null){  
						onOpen.handle();
					}  
				} catch (WebSocketHandshakeException e) { 
					log.error(e.getMessage(), e); 
				}
				return;
			}

			super.channelRead(ctx, msg);
		} 
		

		private String decodeWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
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

		private String parseMessage(ByteBuf buf) {
			int size = buf.readableBytes();
			byte[] data = new byte[size];
			buf.readBytes(data);
			return new String(data, charset);
		} 
	}
}
