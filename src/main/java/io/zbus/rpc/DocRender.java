package io.zbus.rpc;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zbus.kit.FileKit;
import io.zbus.net.http.HttpMessage;
import io.zbus.rpc.RpcProcessor.RpcMethod;

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
		 
		if(!this.rpcProcessor.enableMethodPage){
			result.setBody("<h1>Method page disabled</h1>");
			return result;
		}
		
		String doc = "<div>";
		int rowIdx = 0;
		for(List<RpcMethod> objectMethods : this.rpcProcessor.object2Methods.values()) {
			for(RpcMethod m : objectMethods) {
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
				"<td class=\"returnType\">%s</td>" +  
				"<td class=\"methodParams\"><code><strong><a href=\"%s\">%s</a></strong>(%s)</code>" +  
				"</td>" +
				
				"<td class=\"modules\">" + 
				"	<a href='%s'>%s</a>" + 
				"</td></tr>";
		String methodLink = this.rpcProcessor.docUrlRoot + m.modules.get(0) + "/" + m.name;
		String method = m.name;
		String paramList = "";
		for(String type : m.paramTypes) {
			paramList += type + ", ";
		}
		if(paramList.length() > 0) {
			paramList = paramList.substring(0, paramList.length()-2);
		}  
		String moduleLink = this.rpcProcessor.docUrlRoot + m.modules.get(0);
		
		return String.format(fmt, m.returnType, methodLink, method,
				paramList, moduleLink, m.modules.get(0));
	} 
	
	
}