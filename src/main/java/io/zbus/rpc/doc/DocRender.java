package io.zbus.rpc.doc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import io.zbus.kit.FileKit;
import io.zbus.rpc.RpcMethod;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.http.HttpMessage;

public class DocRender { 
	private final RpcProcessor rpcProcessor;  
	private final String urlPrefix;
	public DocRender(RpcProcessor rpcProcessor, String urlPrefix) {
		this.rpcProcessor = rpcProcessor; 
		this.urlPrefix = urlPrefix;
	}

	public HttpMessage index() throws IOException { 
		HttpMessage result = new HttpMessage(); 
		Map<String, Object> model = new HashMap<String, Object>();
		 
		if(!this.rpcProcessor.isMethodPageEnabled()){
			result.setBody("<h1>Method page disabled</h1>");
			return result;
		}
		
		String doc = "<div>";
		int rowIdx = 0;
		TreeMap<String, Map<String, RpcMethod>> methods = new TreeMap<>(this.rpcProcessor.methodInfoTable);
		Iterator<Entry<String, Map<String, RpcMethod>>> iter = methods.entrySet().iterator();
		while(iter.hasNext()) {
			TreeMap<String, RpcMethod> objectMethods = new TreeMap<>(iter.next().getValue()); 
			for(RpcMethod m : objectMethods.values()) {
				doc += rowDoc(m, rowIdx++);
			}
		} 
		doc += "</div>";
		model.put("content", doc); 
		model.put("urlPrefix", urlPrefix);
		
		String body = FileKit.loadFile("rpc.htm", model);
		result.setBody(body);
		return result;
	}
	
	private String rowDoc(RpcMethod m, int idx) {  
		String fmt = 
				"<tr>" +   
				"<td class=\"module\">" + 
				"	<a href='%s'>%s</a>" + 
				"</td>" +
				"<td class=\"returnType\">%s</td>" +  
				"<td class=\"methodParams\"><code><strong><a href=\"%s\">%s</a></strong>(%s)</code>" +  
				"</td>" + 
				"</tr>";
		String methodLink = urlPrefix + m.module + "/" + m.method;
		String method = m.method;
		String paramList = "";
		int size = m.paramNames.size();
		if(size < m.paramTypes.size()) {
			size = m.paramTypes.size();
		}
		for(int i=0;i<size;i++) { 
			if(i<m.paramTypes.size()) { 
				paramList += m.paramTypes.get(i);
			}
			if(i<m.paramNames.size()) { 
				if(i<m.paramTypes.size()) paramList += " ";
				paramList += m.paramNames.get(i) ;
			}
			paramList += ", ";
		} 
		if(paramList.length() > 0) {
			paramList = paramList.substring(0, paramList.length()-2);
		}  
		String moduleLink = urlPrefix + m.module;
		
		return String.format(fmt, moduleLink, m.module, m.returnType, methodLink, method,
				paramList);
	} 
	
	
}