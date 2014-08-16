package org.zbus.server.mq;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.zbus.remoting.Message;
 
public class MqStore {

	public static void main(String[] args) throws Exception{
		FileOutputStream out = new FileOutputStream("MyMQ");
		ObjectOutputStream objOut = new ObjectOutputStream(out);
		
		StringBuilder sb = new StringBuilder();
		String test = "0123456789";
		for(int i=0;i<500;i++){
			sb.append(test.charAt(i%10));
		}
		String data = sb.toString();
		for(int i=0;i<10000;i++){
			Message msg = new Message();
			msg.setCommand("produce");
			msg.setMq("MyMQ");
			msg.setBody(data);
			objOut.writeObject(msg);  
		}
		objOut.flush(); 
		objOut.close();
		
		FileInputStream in = new FileInputStream("MyMQ");
		ObjectInputStream objIn = new ObjectInputStream(in);
		Message value = (Message)objIn.readObject();
		System.out.println(value);  
		objIn.close();
	}
}
