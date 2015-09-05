package org.zbus.kit.json;

import org.zbus.mq.Protocol.MqMode;

import com.alibaba.fastjson.JSON;


public class FastJsonExample {

	public static void main(String[] args) {   
 		String json = JSON.toJSONString(MqMode.MQ);
 		System.out.println(json);
 		MqMode mode = JSON.parseObject(json, MqMode.class); 
		//MqMode mode = TypeUtils.castToJavaBean(json, MqMode.class);
		System.out.println(mode);
	} 
}
