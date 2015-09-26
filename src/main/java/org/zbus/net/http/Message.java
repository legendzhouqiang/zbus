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
package org.zbus.net.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.kit.log.Logger;
import org.zbus.net.Client.MsgHandler;
import org.zbus.net.MsgInvoker;
import org.zbus.net.Sync.Id;
import org.zbus.net.core.IoBuffer;

/**
 * HTTP Protocol Message, stands for both request and response formats.
 * 
 * Message is NOT standard HTTP protocol, but compatible to HTTP standard format.
 * Message may extend HTTP protocol in the header part, for example: mq: MyMQ\r\n
 * means a header extension of Key=mq, Value=MyMQ.
 * 
 * @author HONG LEIMING
 *
 */
public class Message implements Id {  
	private static final Logger log = Logger.getLogger(Message.class); 
	public static final String HEARTBEAT        = "heartbeat"; //心跳消息
	
	//使用到的标准HTTP头部
	public static final String REMOTE_ADDR      = "remote-addr";
	public static final String CONTENT_LENGTH   = "content-length";
	public static final String CONTENT_TYPE     = "content-type";
	
	//常见扩展HTTP协议头部
	public static final String CMD    	= "cmd"; 
	public static final String SUB_CMD  = "sub_cmd";    
	public static final String MQ       = "mq";
	public static final String SENDER   = "sender"; 
	public static final String RECVER   = "recver";
	public static final String ID      	= "id";	    //消息ID
	public static final String RAWID    = "rawid";  //原始消息ID 
	public static final String SERVER   = "server"; 
	public static final String TOPIC    = "topic";  //使用,分隔 
	public static final String ACK      = "ack";	 	 
	public static final String WINDOW   = "window";  
	public static final String ENCODING = "encoding";
	
	 
	//HTTP协议第一行（请求串或者返回状态码）
	private Meta meta = new Meta(); 
	//HTTP协议Key-Value头部
	private Map<String, String> head = new ConcurrentHashMap<String, String>();
	//HTTP消息体
	private byte[] body; 
	
	public Message(){
		setBody((byte[])null);
	} 
	
	public Message(String body){
		setBody(body); 
	}
	
	public Message(byte[] body){
		setBody(body);
	}
	
	public static Message copyWithoutBody(Message msg){
		Message res = new Message();
		res.meta = new Meta(msg.meta);
		res.head = new ConcurrentHashMap<String, String>(msg.head);
		res.body = msg.body;
		return res;
	}
	
	/**
	 * HTTP请求串
	 * eg. http://localhost/hello?xx=yy
	 * requestString=/hello?xx=yy
	 * @return
	 */
	public String getRequestString(){
		return this.meta.requestString;
	} 
	public void setRequestString(String requestString){
		this.meta.requestString = requestString;
	}
	
	/**
	 * HTTP响应状态码 
	 * e.g. 200 OK
	 * status=200
	 * @return
	 */
	public String getResponseStatus() {  
		return meta.status;
	} 
	public Message setResponseStatus(String status) { 
		meta.status = status;
		return this; 
	} 
	public Message setResponseStatus(int status){
		return setResponseStatus(status+"");
	}
	
	/**
	 * HTTP请求串
	 * eg. http://localhost/hello?xx=yy
	 * requestPath=/hello
	 * @return
	 */
	public String getRequestPath(){
		return this.meta.requestPath;
	}
	 
	public void setRequestPath(String path){
		meta.setRequestPath(path);
	}
	
	public Map<String, String> getRequestParams(){
		return meta.requestParams;
	}
	public String getRequestParam(String key){
		return meta.getRequestParam(key); 
	}  
	public void setRequestParam(String key, String value){
		meta.setRequestParam(key, value);
	}
	
	public Map<String,String> getHead() {
		return head;
	} 
	
	public void setHead(Map<String,String> head) {
		this.head = head;
	} 
	
	public String getHead(String key){
		return this.head.get(key);
	}
	
	public String getHead(String key, String defaultValue){
		String res = this.head.get(key);
		if(res == null) return defaultValue;
		return res;
	}
	
	public void setHead(String key, String value){
		if(value == null) return;
		this.head.put(key, value);
	} 
	
	public String removeHead(String key){
		return this.head.remove(key);
	}
	
	public byte[] getBody() {
		byte[] b = body;
		String bodyOfHead = getHead("body");
		if(b == null && bodyOfHead != null){
			b = bodyOfHead.getBytes();
		}
		return b;
	}
	
	public Message setBody(byte[] body) {
		int len = 0;
		if( body != null){
			len = body.length;
		}
		this.setHead(CONTENT_LENGTH, ""+len);
		this.body = body;
		return this;
	}
	
	public Message setBody(String body, String encoding){
		try {
			return setBody(body.getBytes(encoding));
		} catch (UnsupportedEncodingException e) { //just ignore
			return setBody(body);
		}
	}
	
	public Message setBody(String body){
		return setBody(body.getBytes());
	} 
	
	public Message setBody(String format, Object ...args) { 
		this.setBody(String.format(format, args));
		return this;
	} 
	
	public Message setJsonBody(String body){
		return this.setJsonBody(body.getBytes());
	}
	
	public Message setJsonBody(byte[] body){
		this.setHead(CONTENT_TYPE, "application/json");
		this.setBody(body);
		return this;
	}
	
	public String getBodyString() {
		if (this.getBody() == null) return null;
		return new String(this.getBody());
	}

	public String getBodyString(String encoding) {
		if (this.getBody() == null) return null;
		try {
			return new String(this.getBody(), encoding);
		} catch (UnsupportedEncodingException e) {
			return new String(this.getBody());
		}
	}
	
	//////////////////////////////////////////////////////////////
	
	public void decodeHeaders(byte[] data, int offset, int size){
		ByteArrayInputStream inputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader in = null;
		try{ 
			inputStream = new ByteArrayInputStream(data, offset, size);
			inputStreamReader = new InputStreamReader(inputStream);
			in = new BufferedReader(inputStreamReader);
			String meta = in.readLine();
			if(meta == null) return;
			this.meta = new Meta(meta);
			
			String line = in.readLine();
	        while (line != null && line.trim().length() > 0) {
	            int p = line.indexOf(':');
	            if (p >= 0){ 
	                head.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
	            } 
	            line = in.readLine();
	        }
	       
		} catch(IOException e){ 
			log.error(e.getMessage(), e);
		} finally {
			if(in != null){
				try { in.close(); } catch (IOException e) {}
			}
			if(inputStreamReader != null){
				try { inputStreamReader.close(); } catch (IOException e) {}
			}
			if(inputStream != null){
				try { inputStream.close(); } catch (IOException e) {}
			}
		}
	}
	
	public String getCmd() { 
		return this.getHead(CMD);
	} 
	public Message setCmd(String value) {
		this.setHead(CMD, value); 
		return this;
	}  
	
	
	public String getSubCmd() { 
		return this.getHead(SUB_CMD);
	}  
	public Message setSubCmd(String value) {
		this.setHead(SUB_CMD, value); 
		return this;
	}   
	
	
	public String getServer(){
		return this.getHead(SERVER);
	}  
	public void setServer(String value){
		this.setHead(SERVER, value);
	}
	
	
	public String getSender() {
		return this.getHead(SENDER);
	} 
	public Message setSender(String value) {
		this.setHead(SENDER, value);
		return this;
	}
	
	
	public String getRecver() {
		return this.getHead(RECVER);
	} 
	public Message setRecver(String value) {
		this.setHead(RECVER, value);
		return this;
	}
	
	
	public String getRemoteAddr() {
		return this.getHead(REMOTE_ADDR);
	} 
	public Message setRemoteAddr(String value) {
		this.setHead(REMOTE_ADDR, value);
		return this;
	} 
	
	
	public String getEncoding() {
		return this.getHead(ENCODING);
	} 
	public Message setEncoding(String encoding) {
		this.setHead(ENCODING, encoding);
		return this;
	}
	
	public String getId() {
		return this.getHead(ID);
	} 
	public void setId(String msgId) {
		if(msgId == null) return;
		this.setHead(ID, msgId); 
	}	
	public void setId(long id){
		setId(""+id);
	}
	
	public String getRawId() {
		return this.getHead(RAWID);
	} 
	public Message setRawId(String value) {
		if(value == null) return this;
		this.setHead(RAWID, value);
		return this;
	}
	
	public boolean isAck() {
		String ack = this.getHead(ACK);
		if(ack == null) return true; //default to true
		ack = ack.trim().toLowerCase();
		return ack.equals("1") || ack.equals("true");
	} 
	public void setAck(boolean ack){
		String value = ack? "1":"0";
		this.setHead(ACK, value);
	}
	
	
	public String getMq(){
		String value = this.getHead(MQ);
		return value;
	} 
	public Message setMq(String mq) {
		this.setHead(MQ, mq);
		return this;
	} 
	
	
	public String getTopic() {
		return getHead(TOPIC);
	} 
	public Message setTopic(String topic) {
		this.setHead(TOPIC, topic);
		return this;
	} 
	
	public String getWindow() {
		return getHead(WINDOW);
	} 
	public Message setWindow(int window) {
		this.setHead(WINDOW, ""+window);
		return this;
	} 
	
	
	public boolean isStatus200() {
		return "200".equals(this.getResponseStatus());
	} 
	public boolean isStatus404() {
		return "404".equals(this.getResponseStatus());
	} 
	public boolean isStatus500() {
		return "500".equals(this.getResponseStatus());
	}  
	
	protected String getBodyPrintString() {
		if (this.body == null)
			return null;
		if (this.body.length > 1024) {
			return new String(this.body, 0, 1024) + "...";
		} else {
			return getBodyString();
		}
	}
	
	static final byte[] CLCR = "\r\n".getBytes();
	static final byte[] KV_SPLIT = ": ".getBytes();
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(meta+"\r\n");
		
		List<String> keys = new ArrayList<String>(head.keySet());
		Collections.sort(keys);
		
		for(String key : keys){ 
			String val = head.get(key);
			sb.append(key+": "+val+"\r\n");
		}
		sb.append("\r\n");
		String bodyString = getBodyPrintString();
		if(bodyString != null){
			sb.append(bodyString);
		}
		return sb.toString();
	} 
	
	public byte[] toBytes(){
		IoBuffer bb = toIoBuffer(); 
		byte[] b = new byte[bb.remaining()];
		bb.readBytes(b);
		return b;
	}
	
	public IoBuffer toIoBuffer(){
		IoBuffer bb = meta.toIoBuffer();
		bb.writeBytes(CLCR);
		Iterator<Entry<String, String>> it = head.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, String> kv = it.next();
			bb.writeBytes(kv.getKey().getBytes());
			bb.writeBytes(KV_SPLIT);
			bb.writeBytes(kv.getValue().getBytes());
			bb.writeBytes(CLCR);
		}
		bb.writeBytes(CLCR);
		if(body != null){
			bb.writeBytes(body);
		}
		bb.flip();
		return bb;
	} 

	static class Meta{  
		//HTTP响应头部: 状态(200)
		String status; //根据status是否设置来决定Meta是请求还是应答	
		//HTTP请求头部: 方法(GET/POST)-RequestString-KV参数
		String method = "GET"; 
		
		String requestString = "/";         //请求串 （最终决定，下面两个辅助动态更新）
		String requestPath = requestString; //请求路径
		Map<String,String> requestParams;   //请求参数KV
		
		
		static Set<String> httpMethod = new HashSet<String>();
		static Map<String,String> httpStatus = new HashMap<String, String>();
		
		static{ 
			httpMethod.add("GET");
			httpMethod.add("POST"); 
			httpMethod.add("PUT");
			httpMethod.add("DELETE");
			httpMethod.add("HEAD");
			httpMethod.add("OPTIONS"); 
			
			httpStatus.put("101", "Switching Protocols"); 
			httpStatus.put("200", "OK");
			httpStatus.put("201", "Created");
			httpStatus.put("202", "Accepted");
			httpStatus.put("204", "No Content"); 
			httpStatus.put("206", "Partial Content"); 
			httpStatus.put("301", "Moved Permanently");
			httpStatus.put("304", "Not Modified"); 
			httpStatus.put("400", "Bad Request"); 
			httpStatus.put("401", "Unauthorized"); 
			httpStatus.put("403", "Forbidden");
			httpStatus.put("404", "Not Found"); 
			httpStatus.put("405", "Method Not Allowed"); 
			httpStatus.put("416", "Requested Range Not Satisfiable");
			httpStatus.put("500", "Internal Server Error");
		} 
		
		@Override
		public String toString() { 
			IoBuffer buf = toIoBuffer().flip(); 
			return new String(buf.array(), 0, buf.remaining());
		}
		
		final static byte[] BLANK = " ".getBytes();
		final static byte[] PREFIX = "HTTP/1.1 ".getBytes();
		final static byte[] SUFFIX = " HTTP/1.1".getBytes(); 
		public IoBuffer toIoBuffer(){
			IoBuffer bb = IoBuffer.allocate(1024); 
			if(this.status != null){
				String desc = httpStatus.get(this.status);
				if(desc == null){
					desc = "Unknown Status";
				}
				bb.writeBytes(PREFIX);
				bb.writeString(status);
				bb.writeBytes(BLANK);
				bb.writeString(desc);  
			} else {
				String method = this.method; 
				if(method == null) method = ""; 
				bb.writeString(method);
				bb.writeBytes(BLANK); 
				String requestString = this.requestString;
				if(requestString == null) requestString = "";
				bb.writeString(requestString);
				bb.writeBytes(SUFFIX); 
			}
			return bb;
		}
		
		public Meta(){ }
		
		public Meta(Meta m){
			this.requestString = m.requestString;
			this.requestPath = m.requestPath;
			this.method = m.method;
			this.status = m.status;
			if(m.requestParams != null){
				this.requestParams = new HashMap<String, String>(m.requestParams);
			}
		}
		
		public Meta(String meta){
			if("".equals(meta)){
				return;
			}
			StringTokenizer st = new StringTokenizer(meta);
			String firstWord = st.nextToken();
			if(firstWord.toUpperCase().startsWith("HTTP")){ //理解为响应
				this.status = st.nextToken();
				return;
			}
			//理解为请求
			this.method = firstWord;  
			this.requestString = st.nextToken();
			decodeRequestString(this.requestString);
		} 
		
		public void setMethod(String method){
			this.method = method;
		}
		
		public void setRequestString(String requestString){
			this.requestString = requestString;
			decodeRequestString(requestString);
		} 
		
		private void calcRequestString(){
			StringBuilder sb = new StringBuilder();
			if(this.requestPath != null){ 
				sb.append(this.requestPath);
			}
			if(this.requestParams != null){
				sb.append("?");
				Iterator<Entry<String, String>> it = requestParams.entrySet().iterator();
				while(it.hasNext()){
					Entry<String, String> e = it.next();
					sb.append(e.getKey()+"="+e.getValue());
					if(it.hasNext()){
						sb.append("&&");
					}
				}
			}
			this.requestString = sb.toString(); 
		}
		private void decodeRequestString(String commandString){
			int idx = commandString.indexOf('?');
			if(idx < 0){
				this.requestPath = urlDecode(commandString);
			} else {
				this.requestPath = commandString.substring(0, idx);
			}  
			if(this.requestPath.endsWith("/")){
				this.requestPath = this.requestPath.substring(0, this.requestPath.length()-1);
			}
			if(idx < 0) return;
			if(this.requestParams == null){
				this.requestParams = new ConcurrentHashMap<String, String>(); 
			}
			String paramString = commandString.substring(idx+1); 
			StringTokenizer st = new StringTokenizer(paramString, "&");
	        while (st.hasMoreTokens()) {
	            String e = st.nextToken();
	            int sep = e.indexOf('=');
	            if (sep >= 0) {
	                this.requestParams.put(urlDecode(e.substring(0, sep)).trim(),
	                		urlDecode(e.substring(sep + 1)));
	            } else {
	                this.requestParams.put(urlDecode(e).trim(), "");
	            }
	        } 
		}
		
		private String urlDecode(String str) {
	        String decoded = null;
	        try {
	            decoded = URLDecoder.decode(str, "UTF8");
	        } catch (UnsupportedEncodingException ignored) {
	        }
	        return decoded;
	    }
		
		public String getRequestParam(String key){
			if(requestParams == null) return null;
			return requestParams.get(key);
		}
		
		public String getRequestParam(String key, String defaultValue){
			String value = getRequestParam(key);
			if(value == null){
				value = defaultValue;
			}
			return value;
		}
		
		public void setRequestPath(String path){
			this.requestPath = path;
			calcRequestString();
		}
		
		public void setRequestParam(String key, String value){
			if(requestParams == null){
				requestParams = new HashMap<String, String>();
			}
			requestParams.put(key, value);
			calcRequestString();
		}
	}

	public static interface MessageHandler extends MsgHandler<Message> { }
	public static interface MessageInvoker extends MsgInvoker<Message, Message> { }	
	public static interface MessageProcessor { 
		Message process(Message request);
	}
}