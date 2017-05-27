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
package io.zbus.mq;
 

import static io.zbus.mq.Protocol.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class Message {  
	private static final Logger log = LoggerFactory.getLogger(Message.class); 
	private static final String DEFAULT_ENCODING = "UTF-8"; 
	
	public static final String HEARTBEAT        = "heartbeat";  
	
	public static final String REMOTE_ADDR      = "remote-addr";
	public static final String CONTENT_LENGTH   = "content-length";
	public static final String CONTENT_TYPE     = "content-type";    
	public static final String CONTENT_TYPE_BINARY   = "application/octet-stream"; 
	public static final String CONTENT_TYPE_JSON     = "application/json"; 
	 
	private Integer status; //null: request, otherwise: response
	private String url = "/";
	private String method = "GET"; 
	
	private Map<String, String> headers = new ConcurrentHashMap<String, String>(); 
	private byte[] body; 
	
	public Message(){
		setBody((byte[])null);
		setCommonHeaders();
	} 
	
	public Message(String body){
		setBody(body); 
		setCommonHeaders();
	}
	
	public Message(byte[] body){
		setBody(body);
		setCommonHeaders();
	}
	
	private void setCommonHeaders(){
		setHeader("connection", "Keep-Alive");
		setVersion(Protocol.VERSION_VALUE);
	}
	
	public static Message copyWithoutBody(Message msg){
		Message res = new Message();
		res.status = msg.status;
		res.url = msg.url;
		res.method = msg.method;
		res.headers = new ConcurrentHashMap<String, String>(msg.headers);
		res.body = msg.body;
		return res;
	}
	 
	public String getUrl(){
		return this.url;
	} 
	
	public Message setUrl(String url){ 
		this.url = url;  
		return this;
	}
	
	public Message setStatus(Integer status) { 
		this.status = status;
		return this; 
	} 
	
	public Integer getStatus(){
		return status;
	} 
	
	public String getMethod(){
		return this.method;
	}
	
	public void setMethod(String method){
		this.method = method;
	} 
	
	public Map<String,String> getHeaders() {
		return headers;
	} 
	
	public void setHeaders(Map<String,String> head) {
		this.headers = head;
	} 
	
	public String getHeader(String key){
		return this.headers.get(key);
	}
	
	public String getHeader(String key, String defaultValue){
		String res = this.headers.get(key);
		if(res == null) return defaultValue;
		return res;
	}
	
	public void setHeader(String key, String value){
		if(value == null) return;
		this.headers.put(key, value);
	} 
	
	public void setHeader(String key, Object value){
		if(value == null) return;
		this.headers.put(key, value.toString());
	} 
	
	public String removeHeader(String key){
		return this.headers.remove(key);
	}
	
	public byte[] getBody() {
		byte[] b = body;
		String bodyOfHead = getHeader("body");
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
		this.setHeader(CONTENT_LENGTH, ""+len); 
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
		String encoding = this.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		}
		try {
			return setBody(body.getBytes(encoding));
		} catch (UnsupportedEncodingException e) { //just ignore
			return setBody(body.getBytes());
		}
	} 
	
	public Message setBody(String format, Object ...args) { 
		this.setBody(String.format(format, args));
		return this;
	}  
	
	public Message setJsonBody(String body){
		return this.setJsonBody(body.getBytes());
	}
	
	public Message setJsonBody(byte[] body){ 
		this.setBody(body);
		this.setHeader(CONTENT_TYPE, CONTENT_TYPE_JSON);
		return this;
	}
	
	public String getBodyString() {
		if (this.getBody() == null) return null;
		String encoding = this.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		}
		return getBodyString(encoding);
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
	public void decodeHeaders(InputStream in){ 
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		try{  
			inputStreamReader = new InputStreamReader(in);
			bufferedReader = new BufferedReader(inputStreamReader);
			String meta = bufferedReader.readLine();
			if(meta == null) return;

			StringTokenizer st = new StringTokenizer(meta);
			String start = st.nextToken();
			if(start.toUpperCase().startsWith("HTTP")){ //As response
				this.status = Integer.valueOf(st.nextToken()); 
			} else {
				this.method = start;  
				this.url = st.nextToken();
			}
			
			String line = bufferedReader.readLine();
	        while (line != null && line.trim().length() > 0) {
	            int p = line.indexOf(':');
	            if (p >= 0){ 
	                headers.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
	            } 
	            line = bufferedReader.readLine();
	        }
	       
		} catch(IOException e){ 
			log.error(e.getMessage(), e);
		} finally {
			if(bufferedReader != null){
				try { bufferedReader.close(); } catch (IOException e) {}
			}
			if(inputStreamReader != null){
				try { inputStreamReader.close(); } catch (IOException e) {}
			} 
		}
	}
	
	public void decodeHeaders(byte[] data, int offset, int size){
		ByteArrayInputStream in = new ByteArrayInputStream(data, offset, size);
		decodeHeaders(in);
		if(in != null){
			try { in.close(); } catch (IOException e) {}
		} 
	}
	
	public String getVersion(){
		return this.getHeader(VERSION); 
	} 
	public Message setVersion(String value) {
		this.setHeader(VERSION, value);
		return this;
	} 
	
	public String getCommand() { 
		return this.getHeader(COMMAND);
	} 
	public Message setCommand(String value) {
		this.setHeader(COMMAND, value); 
		return this;
	}   
	
	public String getHost(){
		return this.getHeader(HOST);
	}  
	public void setHost(String value){
		this.setHeader(HOST, value);
	} 
	
	public String getSender() {
		return this.getHeader(SENDER);
	} 
	public Message setSender(String value) {
		this.setHeader(SENDER, value);
		return this;
	}
	
	
	public String getReceiver() {
		return this.getHeader(RECVER);
	} 
	public Message setReceiver(String value) {
		this.setHeader(RECVER, value);
		return this;
	} 
	
	public String getToken() {
		return this.getHeader(TOKEN);
	} 
	public Message setToken(String value) {
		this.setHeader(TOKEN, value);
		return this;
	}   
	
	public String getTag() {
		return this.getHeader(TAG);
	} 
	public Message setTag(String value) {
		this.setHeader(TAG, value);
		return this;
	}   
	
	public String getRemoteAddr() {
		return this.getHeader(REMOTE_ADDR);
	} 
	public Message setRemoteAddr(String value) {
		this.setHeader(REMOTE_ADDR, value);
		return this;
	}  
	
	
	public Integer getOriginStatus() {
		String value = this.getHeader(ORIGIN_STATUS);
		if(value == null) return null;
		return Integer.valueOf(value);
	} 
	public Message setOriginStatus(Integer value) {
		this.setHeader(ORIGIN_STATUS, value);
		return this;
	}  
	
	public String getOriginUrl() {
		return this.getHeader(ORIGIN_URL);
	} 
	public Message setOriginUrl(String value) {
		this.setHeader(ORIGIN_URL, value);
		return this;
	}   
	
	public String getOriginId() {
		return this.getHeader(ORIGIN_ID);
	} 
	public Message setOriginId(String value) {
		if(value == null) return this;
		this.setHeader(ORIGIN_ID, value);
		return this;
	}
	 
	
	public String getEncoding() {
		return this.getHeader(ENCODING);
	} 
	public Message setEncoding(String encoding) {
		this.setHeader(ENCODING, encoding);
		return this;
	} 
	
	public String getId() {
		return this.getHeader(ID);
	} 
	
	public void setId(String msgId) {
		if(msgId == null) return;
		this.setHeader(ID, msgId); 
	}	 
	
	public boolean isAck() {
		String ack = this.getHeader(ACK);
		if(ack == null) return true; //default to true
		ack = ack.trim().toLowerCase();
		return ack.equals("1") || ack.equals("true");
	} 
	
	public void setAck(boolean ack){
		String value = ack? "1":"0";
		this.setHeader(ACK, value);
	} 
	
	public String getTopic(){
		String value = this.getHeader(TOPIC);
		return value;
	} 
	public Message setTopic(String mq) {
		this.setHeader(TOPIC, mq);
		return this;
	} 
	
	public String getConsumeGroup(){
		String value = this.getHeader(CONSUME_GROUP);
		return value;
	} 
	public Message setConsumeGroup(String value) {
		this.setHeader(CONSUME_GROUP, value);
		return this;
	} 
	public Long getOffset(){
		String value = this.getHeader(OFFSET);
		if(value == null) return null;
		return Long.valueOf(value);
	} 
	public Message setOffset(Long value) {
		this.setHeader(OFFSET, value);
		return this;
	} 
	
	public Integer getTopicMask(){
		String value = this.getHeader(TOPIC_MASK);
		if(value == null) return null;
		return Integer.valueOf(value);
	} 
	public Message setTopicMask(Integer value) {
		this.setHeader(TOPIC_MASK, value);
		return this;
	} 
	
	public Integer getGroupMask(){
		String value = this.getHeader(GROUP_MASK);
		if(value == null) return null;
		return Integer.valueOf(value);
	} 
	public Message setGroupMask(Integer value) {
		this.setHeader(GROUP_MASK, value);
		return this;
	} 
	
	public Long getGroupStartOffset(){
		String value = this.getHeader(GROUP_START_OFFSET);
		if(value == null) return null;
		return Long.valueOf(value);
	} 
	public Message setGroupStartOffset(Long value) {
		this.setHeader(GROUP_START_OFFSET, value);
		return this;
	}   
	public Long getGroupStartTime(){
		String value = this.getHeader(GROUP_START_TIME);
		if(value == null) return null;
		return Long.valueOf(value);
	} 
	public Message setGroupStartTime(Long value) {
		this.setHeader(GROUP_START_TIME, value);
		return this;
	}  
	public String getGroupStartCopy(){
		return this.getHeader(GROUP_START_COPY);
	} 
	public Message setGroupStartCopy(String value) {
		this.setHeader(GROUP_START_COPY, value);
		return this;
	}  
	
	public String getGroupStartMsgId(){
		String value = this.getHeader(GROUP_START_MSGID);
		return value;
	} 
	public Message setGroupStartMsgId(String mq) {
		this.setHeader(GROUP_START_MSGID, mq);
		return this;
	} 
	
	public Integer getConsumeWindow(){
		String value = this.getHeader(CONSUME_WINDOW);
		if(value == null) return null;
		return Integer.valueOf(value);
	} 
	
	public Message setConsumeWindow(Integer value) {
		this.setHeader(CONSUME_WINDOW, value);
		return this;
	}  
	public String getGroupFilter() {
		return getHeader(GROUP_FILTER);
	} 
	
	public Message setGroupFilter(String value) {
		this.setHeader(GROUP_FILTER, value);
		return this;
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
		sb.append(generateHttpLine()+"\r\n");
		
		List<String> keys = new ArrayList<String>(headers.keySet());
		Collections.sort(keys);
		
		for(String key : keys){ 
			String val = headers.get(key);
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
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeTo(out);
		} catch (IOException e) {
			return null;
		}
		return out.toByteArray(); 
	} 
	
	private static int findHeaderEnd(byte[] data){ 
		int i = 0;
		int limit = data.length;
		while(i+3<limit){
			if(data[i] != '\r') {
				i += 1;
				continue;
			}
			if(data[i+1] != '\n'){
				i += 1;
				continue;
			}
			
			if(data[i+2] != '\r'){
				i += 3;
				continue;
			}
			
			if(data[i+3] != '\n'){
				i += 3;
				continue;
			}
			
			return i+3; 
		}
		return -1;
	}
	
	public static Message parse(byte[] data){
		int idx = findHeaderEnd(data);
		if(idx == -1){
			throw new IllegalArgumentException("Invalid input byte array");
		}
		int headLen = idx + 1;
		Message msg = new Message();
		msg.decodeHeaders(data, 0, headLen);
		String contentLength = msg.getHeader(Message.CONTENT_LENGTH);
		if(contentLength == null){ //just head 
			return msg;
		}
		
		int bodyLen = Integer.valueOf(contentLength);   
		if(data.length != headLen + bodyLen) {
			throw new IllegalArgumentException("Invalid input byte array");
		}
		byte[] body = new byte[bodyLen];
		System.arraycopy(data, headLen, body, 0, bodyLen);
		msg.setBody(body); 
		
		return msg; 
	}
	
	private final static byte[] BLANK = " ".getBytes();
	private final static byte[] PREFIX = "HTTP/1.1 ".getBytes();
	private final static byte[] SUFFIX = " HTTP/1.1".getBytes(); 
	
	private String generateHttpLine(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeHttpLine(out);
		} catch (IOException e) { 
			return null;
		}
		return out.toString();
	}
	
	private void writeHttpLine(OutputStream out) throws IOException{
		if(this.status != null){
			String desc = null;
			HttpResponseStatus s = HttpResponseStatus.valueOf(Integer.valueOf(status));
			if(s != null){
				desc = s.reasonPhrase();
			} else {
				desc = "Unknown Status";
			}
			out.write(PREFIX);
			out.write(String.format("%d", status).getBytes());
			out.write(BLANK);
			out.write(desc.getBytes());  
		} else {
			String method = this.method; 
			if(method == null) method = ""; 
			out.write(method.getBytes());
			out.write(BLANK); 
			String requestString = this.url;
			if(requestString == null) requestString = "/";
			out.write(requestString.getBytes());
			out.write(SUFFIX); 
		}
	} 
	
	public void writeTo(OutputStream out) throws IOException{
		writeHttpLine(out);
		out.write(CLCR);
		Iterator<Entry<String, String>> it = headers.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, String> kv = it.next();
			out.write(kv.getKey().getBytes());
			out.write(KV_SPLIT);
			out.write(kv.getValue().getBytes());
			out.write(CLCR);
		}
		out.write(CLCR);
		if(body != null){
			out.write(body);
		}
	} 
	
	public void urlToHead(){ 
		int idx = url.lastIndexOf('?');
		if(idx < 0){
			return;
		} 
		String paramString = url.substring(idx+1); 
		StringTokenizer st = new StringTokenizer(paramString, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            if (sep >= 0) {
            	String key = e.substring(0, sep).trim().toLowerCase();
            	String val = e.substring(sep + 1).trim(); 
            	if(!this.headers.containsKey(key)){
            		this.headers.put(key, val);
            	}
            }  
        }  
	} 
}