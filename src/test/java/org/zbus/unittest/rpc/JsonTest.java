package org.zbus.unittest.rpc;

import com.alibaba.fastjson.JSON;

public class JsonTest {

	public static void main(String[] args) { 
		byte[] data = new byte[10];
		String dataString = JSON.toJSONString(data);
		byte[] res = JSON.parseObject(dataString, byte[].class);
		System.out.println(res);
	}

}
