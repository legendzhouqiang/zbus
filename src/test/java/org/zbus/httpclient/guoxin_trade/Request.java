package org.zbus.httpclient.guoxin_trade;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class Request{ 
	public String tradeNodeId = "";  
	public String sessionId = ""; 
	public String funcId = ""; 
	public String userInfo = "";
	 
	
	public String loginType = ""; 
	public String loginId = ""; 
	public String custOrg = ""; 
	public String operIp = ""; 
	public String operOrg = "yyt"; 
	public String operType = "g"; 
	public String authId = ""; 
	public String accessIp = ""; 
	public String extention1 = ""; 
	public String extention2 = ""; 	
	public List<String> params = new ArrayList<String>();
	
	
	public Request(){
		try {
			this.operIp = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) { 
			e.printStackTrace();
			this.operIp = "127.0.0.1";
		}
	}
	
	public Request(String funcNo, String ipAddress, 
			String tradeNode, String authId, 
			String branchCode, String loginId){
	    this.funcId = funcNo;
	    this.tradeNodeId = tradeNode;  
	    this.userInfo = "0~hdpt~" + ipAddress + "~" + branchCode;
	    this.loginId = loginId; 
	    this.loginType = "Z";
	    this.custOrg = branchCode;
	    this.operIp = ipAddress; 
	    this.operOrg = branchCode; 
	    this.operType = "";
	    this.authId = "";
	    this.accessIp = "HDZX";
	    this.extention1 = "";
	    this.extention2 = ""; 
	}

	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.tradeNodeId);
		sb.append("|");
		sb.append(this.sessionId);
		sb.append("|");
		sb.append(this.funcId);
		sb.append("|");
		sb.append(this.userInfo); 
		sb.append("|");
		
		sb.append(";");//head;body
		 
		sb.append(this.funcId);
		sb.append("|");
		sb.append(this.loginType);
		sb.append("|");
		sb.append(this.loginId);
		sb.append("|");
		sb.append(this.custOrg);
		sb.append("|");
		sb.append(this.operIp);
		sb.append("|");
		sb.append(this.operOrg);
		sb.append("|");
		sb.append(this.operType);
		sb.append("|");
		sb.append(this.authId);
		sb.append("|");
		sb.append(this.accessIp);
		sb.append("|");
		sb.append(this.extention1);
		sb.append("|");
		sb.append(this.extention2);
		sb.append("|");
		for(int i=0;i<this.params.size();i++){
			sb.append(this.params.get(i)); 
			sb.append("|");
		} 
		
		return sb.toString();
	}
	
	public static Request load(String message){
		Request request = new Request();
		String[] parts = message.split(";");
		if(parts.length != 2){
			throw new RuntimeException("trade message should be ; seperated");
		}
		
		String headPart = parts[0];
		String bodyPart = parts[1];
		
		parts = headPart.split("[|]", -1);
		if(parts.length != 4){
			throw new RuntimeException("trade request head should be 4 parts(|)");
		}
		request.tradeNodeId = parts[0];
		request.sessionId = parts[1];
		request.funcId = parts[2];
		request.userInfo = parts[3];
		
		
		parts = bodyPart.split("[|]",-1);
		if(parts.length < 11){
			throw new RuntimeException("trade body should be  at least 11 parts(|)");
		}
		request.funcId = parts[0];
		request.loginType = parts[1];
		request.loginId = parts[2];
		request.custOrg = parts[3];
		request.operIp = parts[4];
		request.operOrg = parts[5];
		request.operType = parts[6];
		request.authId = parts[7];
		request.accessIp = parts[8];
		request.extention1 = parts[9];
		request.extention2 = parts[10];
		
		for(int i=11; i< parts.length; i++){
			request.params.add(parts[i]);
		}
		
		return request;
	}
}