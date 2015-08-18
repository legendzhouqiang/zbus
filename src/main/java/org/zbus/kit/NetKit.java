package org.zbus.kit;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetKit {   
	
	public static String normalizeAddress(String address){
		String[] blocks = address.split("[:]");
		if(blocks.length > 2){
			throw new IllegalArgumentException(address + " is invalid");
		}
		String host = blocks[0];
		int port = 80;
		if(blocks.length > 1){
			port = Integer.valueOf(blocks[1]);
		} else {
			address += ":"+port; //use default 80
		} 
		String serverAddr = String.format("%s:%d", host, port);
		return serverAddr;
	}
	
	public static String getLocalAddress(String address){
		String[] blocks = address.split("[:]");
		if(blocks.length != 2){
			throw new IllegalArgumentException(address + " is invalid address");
		} 
		String host = blocks[0];
		int port = Integer.valueOf(blocks[1]);
		
		if("0.0.0.0".equals(host)){
			return String.format("%s:%d",NetKit.getLocalIp(), port);
		}
		return address;
	}
	
	public static String getLocalIp() {
		try {
			Pattern pattern = Pattern.compile("(192|172|10)\\.[0-9]+\\.[0-9]+\\.[0-9]+");
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				Enumeration<InetAddress> en = ni.getInetAddresses();
				while (en.hasMoreElements()) {
					InetAddress addr = en.nextElement();
					String ip = addr.getHostAddress();
					Matcher matcher = pattern.matcher(ip);
					if (matcher.matches()) {
						return ip;
					}
				}
			} 
			return "0.0.0.0";
		} catch (Throwable e) {
			e.printStackTrace(); 
			return "0.0.0.0";
		}
	}
  
	public static String remoteAddress(SocketChannel channel){
		SocketAddress addr = channel.socket().getRemoteSocketAddress();
		String res = String.format("%s", addr);
		return res;
	}
	
	public static String localAddress(SocketChannel channel){
		SocketAddress addr = channel.socket().getLocalSocketAddress();
		String res = String.format("%s", addr);
		return addr==null? res: res.substring(1);
	}
}
