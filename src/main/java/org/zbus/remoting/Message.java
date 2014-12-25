package org.zbus.remoting;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
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
 


public class Message implements Serializable { 
	private static final long serialVersionUID = 4379223525215626137L;

	public static final String HEARTBEAT         = "heartbeat"; //心跳消息
	
	//标准HTTP头部内容
	public static final String HEADER_REMOTE_ADDR= "remote-addr";
	public static final String HEADER_ENCODING   = "content-encoding";
	
	//常见扩展HTTP协议头部
	public static final String HEADER_BROKER     = "broker";
	public static final String HEADER_REMOTE_ID  = "remote-id"; 
	public static final String HEADER_TOPIC      = "topic"; //使用,分隔 
	public static final String HEADER_MQ_REPLY   = "mq-reply";
	public static final String HEADER_MQ         = "mq";
	public static final String HEADER_TOKEN      = "token";
	public static final String HEADER_MSGID      = "msgid";	 //消息ID
	public static final String HEADER_MSGID_SRC  = "msgid-raw"; //原始消息ID
	public static final String HEADER_ACK        = "ack";	
	public static final String HEADER_REPLY_CODE = "reply-code";	 
	public static final String HEADER_WINDOW     = "window";	
	
	
	 
	protected Meta meta = new Meta();
	protected Map<String, String> head = new ConcurrentHashMap<String, String>();
	protected byte[] body; 
	
	
	
	public Message(){
		setBody((byte[])null);
	} 
	
	public static Message copyWithoutBody(Message msg){
		Message res = new Message();
		res.meta = new Meta(msg.meta);
		res.head = new HashMap<String, String>(msg.head);
		res.body = msg.body;
		return res;
	}
	
	public String getMetaString() {
		return meta.toString();
	}
	
	public Meta getMeta(){
		return meta;
	}
	
	public void setMeta(String meta) { 
		this.meta = new Meta(meta);
	}
	
	public void setMeta(Meta meta) { 
		this.meta = meta;
	}
	
	public Map<String, String> getHead() {
		return head;
	} 
	
	public void setHead(Map<String, String> head) {
		this.head = head;
	} 
	
	public String getHead(String key){
		return this.head.get(key);
	}
	
	public void setHead(String key, String value){
		if(value == null) return;
		this.head.put(key, value);
	} 
	
	public String removeHead(String key){
		return this.head.remove(key);
	}
	
	public String getParam(String key){
		return meta.getParam(key); 
	}
	
	public String getHeadOrParam(String key){ 
		String value = getHead(key);
		if(value == null){
			value = getParam(key); 
		} 
		return value;
	}
	
	public String getHeadOrParam(String key, String defaultValue) { 
		String value = getHeadOrParam(key);
		if(value == null){
			value = defaultValue;
		}
		return value;
	}   
	
	public byte[] getBody() {
		return body;
	}
	
	public void setBody(byte[] body) {
		int len = 0;
		if( body != null){
			len = body.length;
		}
		this.setHead("content-length", ""+len);
		this.body = body;
	}
	
	public void setBody(String body){
		setBody(body.getBytes());
	} 
	
	public Message setBody(String format, Object ...args) { 
		this.setBody(String.format(format, args));
		return this;
	} 
	
	public void setJsonBody(String body){
		this.setJsonBody(body.getBytes());
	}
	
	public void setJsonBody(byte[] body){
		this.setHead("content-type", "application/json");
		this.setBody(body);
	}
	
	public String getBodyString() {
		if (this.body == null) return null;
		return new String(this.body);
	}

	public String getBodyString(String encoding) {
		if (this.body == null) return null;
		try {
			return new String(this.body, encoding);
		} catch (UnsupportedEncodingException e) {
			return new String(this.body);
		}
	}
	
	//////////////////////////////////////////////////////////////
	
	public void decodeHeaders(byte[] data, int offset, int size){
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(new 
				ByteArrayInputStream(data, offset, size)));
	
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
			e.printStackTrace();
		}
	}
	
	public int estimatedSize(){
		int size = 0;
		size += meta.toString().length();
		
		Iterator<Entry<String, String>> iter = head.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, String> e = iter.next();
			size += e.getKey().length() + e.getKey().length();
		} 
		
		if(body != null){
			size += body.length;
		}
		return size;
	} 
	
	public String getMqReply() {
		return this.getHeadOrParam(HEADER_MQ_REPLY);
	}
	public Message setMqReply(String value) {
		this.setHead(HEADER_MQ_REPLY, value);
		return this;
	}
	
	public String getEncoding() {
		return this.getHeadOrParam(HEADER_ENCODING);
	}
	public Message setEncoding(String encoding) {
		this.setHead(HEADER_ENCODING, encoding);
		return this;
	}
	
	
	public String getMsgId() {
		return this.getHeadOrParam(HEADER_MSGID);
	}
	public Message setMsgId(String msgId) {
		if(msgId == null) return this;
		this.setHead(HEADER_MSGID, msgId);
		return this;
	}
	
	
	public String getMsgIdSrc() {
		return this.getHeadOrParam(HEADER_MSGID_SRC);
	}
	public Message setMsgIdSrc(String value) {
		if(value == null) return this;
		this.setHead(HEADER_MSGID_SRC, value);
		return this;
	}
	
	public boolean isAck() {
		String ack = this.getHeadOrParam(HEADER_ACK);
		if(ack == null) return true; //默认ack为true
		ack = ack.trim().toLowerCase();
		return ack.equals("1");
	}
	
	public void setAck(boolean ack){
		String value = ack? "1":"0";
		this.setHead(HEADER_ACK, value);
	}
	
	public String getMq(){
		return this.getHeadOrParam(HEADER_MQ);
	}
	 
	public Message setMq(String mq) {
		this.setHead(HEADER_MQ, mq);
		return this;
	} 
	
	public String getToken() {
		return this.getHeadOrParam(HEADER_TOKEN);
	}
	public Message setToken(String token) {
		this.setHead(HEADER_TOKEN, token);
		return this;
	} 
	
	public String getTopic() {
		return getHeadOrParam(HEADER_TOPIC);
	}

	public Message setTopic(String topic) {
		this.setHead(HEADER_TOPIC, topic);
		return this;
	} 
	
	public String getWindow() {
		return getHeadOrParam(HEADER_WINDOW);
	}

	public Message setWindow(int window) {
		this.setHead(HEADER_WINDOW, ""+window);
		return this;
	} 
	

	public String getRemoteId() {
		return getHeadOrParam(HEADER_REMOTE_ID);
	}

	public Message setRemoteId(String value) {
		this.setHead(HEADER_REMOTE_ID, value);
		return this;
	}   


	//////////特殊处理：Command 与 Status 兼容HTTP////////////// 
	public String getCommand() { //特殊处理 
		return meta.command; 
	} 
	public Message setCommand(String command) {
		meta.command = command;
		meta.status = null; //互斥
		return this;
	}   
	public String getStatus() {  
		return meta.status;
	}
	public Message setStatus(String status) { 
		meta.status = status;
		meta.command = null; //互斥
		return this; 
	}
	
	public boolean isStatus200() {
		return "200".equals(this.getStatus());
	}

	public boolean isStatus404() {
		return "404".equals(this.getStatus());
	}

	public boolean isStatus500() {
		return "500".equals(this.getStatus());
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
		sb.append(getBodyPrintString());
		return sb.toString();
	} 
	 
}



class Meta implements Serializable{ 
	private static final long serialVersionUID = -8557063231118504061L;
	//HTTP请求头部: 方法(GET/POST)-命令-参数
	String method = "GET"; 
	String command; 
	Map<String,String> params;
	
	//HTTP响应头部: 状态(200)
	String status;
	
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
		if(this.status != null){ //status存在，则认为是响应元数据
			String desc = httpStatus.get(this.status);
			if(desc == null){
				desc = "Unknown Status";
			}
			return String.format("HTTP/1.1 %s %s", status, desc); 
		}
		
		if(this.command != null){//command存在，则认为是请求元数据
			String cmdStr = encodeCommand();
			return String.format("%s /%s HTTP/1.1", method, cmdStr);
		}
		
		return "";
	}
	
	public Meta(){}
	
	public Meta(Meta m){
		this.command = m.command;
		this.method = m.method;
		this.status = m.status;
		if(m.params != null){
			this.params = new HashMap<String, String>(m.params);
		}
	}
	
	public Meta(String meta){
		if("".equals(meta)){
			return;
		}
		StringTokenizer st = new StringTokenizer(meta);
		String method = st.nextToken();
		
		if(!httpMethod.contains(method)){ //不是method字段，理解为Response
			String status = st.nextToken();
			this.status = status;
			return;
		}
		
		this.method = method;  
		String commandString = st.nextToken();
		decodeCommand(commandString);
	} 
	

	private String encodeCommand(){
		StringBuilder sb = new StringBuilder();
		if(this.command != null){
			sb.append(this.command);
		}
		if(this.params == null) return sb.toString();
		
		Iterator<Entry<String, String>> iter = this.params.entrySet().iterator();
		if(iter.hasNext()){
			sb.append("?");
		}
		
		while(iter.hasNext()){
			Entry<String, String> e = iter.next();
			sb.append(e.getKey()+"="+e.getValue());
			if(iter.hasNext()){
				sb.append("&");
			}
		}
		return sb.toString();
	} 
	
	
	private void decodeCommand(String commandString){
		int idx = commandString.indexOf('?');
		if(idx < 0){
			this.command = decodeUrl(commandString);
		} else {
			this.command = commandString.substring(0, idx);
		}
		if(this.command.startsWith("/")){
			this.command = this.command.substring(1);
		}
		if(idx < 0) return;
		
		this.params = new HashMap<String, String>(); 
		String paramString = commandString.substring(idx+1); 
		StringTokenizer st = new StringTokenizer(paramString, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            if (sep >= 0) {
                this.params.put(decodeUrl(e.substring(0, sep)).trim(),
                		decodeUrl(e.substring(sep + 1)));
            } else {
                this.params.put(decodeUrl(e).trim(), "");
            }
        } 
	}
	
	private String decodeUrl(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return decoded;
    }
	
	public String getParam(String key){
		if(params == null) return null;
		return params.get(key);
	}
	
	public String getParam(String key, String defaultValue){
		String value = getParam(key);
		if(value == null){
			value = defaultValue;
		}
		return value;
	}
	
	public void setParam(String key, String value){
		if(value == null) return;
		if(params == null){
			params = new HashMap<String, String>();
		}
		params.put(key, value);
	}
}