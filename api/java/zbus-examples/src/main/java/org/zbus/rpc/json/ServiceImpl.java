package org.zbus.rpc.json;

import org.zbus.client.rpc.Remote;

public class ServiceImpl implements ServiceInterface {
	
	@Remote
	public String echo(String ping) {  
		return ping;
	}
	
	@Remote
	public String echo() { 
		return "my echo";
	}
  

	@Remote
	public int plus(int a, int b) {
		return a + b;
	}

	@Remote
	public byte[] bin() {
		byte[] res = new byte[100];
		for (int i = 0; i < res.length; i++)
			res[i] = (byte) i;
		return res;
	}
}
 