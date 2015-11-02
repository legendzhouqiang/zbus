package org.zbus.mq.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UrlInfo{
	public String cmd;
	public String mq;
	public String module;
	public String method;
	public String params;
	public Map<String, String> extra;
	public boolean empty = false;
	
	public UrlInfo(String url){
		this(url, false);
	}
	
	public UrlInfo(String url, boolean directRpc){
		if(url == null || "".equals(url)){
			empty = true;
			return;
		}
		if(url.length() == 1 && url.charAt(0) == '/'){ 
			empty = true;
			return;
		}
		try {
			url = URLDecoder.decode(url, "UTF8");
		} catch (UnsupportedEncodingException e) {
			return;
		}
		String kvs;
		boolean hasKvs = url.indexOf('?') != -1;
		String[] bb = url.split("[/?]+");  
		if(hasKvs){
			kvs = bb[bb.length-1]; 
			if(!directRpc){
				if(bb.length >= 5){
					mq = bb[1];
					module = bb[2];
					method = bb[3];
				} else  if(bb.length >= 4){
					mq = bb[1];
					method = bb[2];
				} else  if(bb.length >= 3){
					mq = bb[1];
				}
			} else {
				if(bb.length >= 4){ 
					module = bb[1];
					method = bb[2];
				} else  if(bb.length >= 3){
					method = bb[1]; 
				} 
			}
		} else {
			if(!directRpc){
				if(bb.length >= 4){
					mq = bb[1];
					module = bb[2];
					method = bb[3];
				} else  if(bb.length >= 3){
					mq = bb[1];
					method = bb[2];
				} else  if(bb.length >= 2){
					mq = bb[1];
				}
			} else {
				if(bb.length >= 3){ 
					module = bb[1];
					method = bb[2];
				} else  if(bb.length >= 2){
					method = bb[1]; 
				} 
			}
			return;
		} 
		
		bb = kvs.split("[&]+"); 
		for(String b : bb){
			if("".equals(b)) continue;
			int idx =  b.indexOf('=');
			if(idx < 0){
				if(params == null || params.equals("")){
					params = b;
				}
				continue;
			}
			String key = b.substring(0, idx);
			String val = b.substring(idx+1);
			if(extra == null){
				synchronized (this) {
					if(extra == null){
						extra = new ConcurrentHashMap<String, String>();
					}
				} 
			}
			extra.put(key, val);
		} 
		if(extra != null){
			if(cmd == null && extra.containsKey("cmd")){
				cmd = extra.get("cmd");
			}
			if(mq == null && extra.containsKey("mq")){
				mq = extra.get("mq");
			}
			if(method == null && extra.containsKey("method")){
				method = extra.get("method");
			}
			if(params == null && extra.containsKey("params")){
				params = extra.get("params");
			}
		}
	}   

	@Override
	public String toString() {
		return "UrlInfo [cmd=" + cmd + ", mq=" + mq + ", module=" + module
				+ ", method=" + method + ", params=" + params + ", extra="
				+ extra + "]";
	} 
	
	public static void main(String[] args){
		UrlInfo url = new UrlInfo("/t2/xxx?ok", true);
		System.out.println(url);
	}

}
