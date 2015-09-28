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
			Pattern pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			String condidate = null;
			while (interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				Enumeration<InetAddress> en = ni.getInetAddresses(); 
				while (en.hasMoreElements()) {
					InetAddress addr = en.nextElement();
					String ip = addr.getHostAddress(); 
					Matcher matcher = pattern.matcher(ip);
					if (matcher.matches()) { 
						if(ip.startsWith("127.")) continue;  
						if(ip.startsWith("10.")){
							condidate = ip;
							continue;
						} 
						if(ip.startsWith("172.")){
							if(!condidate.startsWith("10.")){
								condidate = ip;
							}
							continue;
						}
						if(ip.startsWith("192.")){
							if(!condidate.startsWith("10.") && !condidate.startsWith("172.")){
								condidate = ip;
							}
							continue;
						}
						
						return ip;
					} 
				} 
			} 
			if(condidate != null) return condidate;
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
