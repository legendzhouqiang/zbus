package org.zbus.server.mq;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentMap;

import org.zbus.logging.Logger;
import org.zbus.logging.LoggerFactory;
import org.zbus.remoting.Message;
 
public class MqStore {
	private static final Logger log = LoggerFactory.getLogger(MqStore.class);
	private static final String DUMP_FILE = "zbus.dump";
	private ConcurrentMap<String, AbstractMQ> mqTable; 
	public MqStore(ConcurrentMap<String, AbstractMQ> mqTable){
		this.mqTable = mqTable; 
	} 
	
	public void dump(){ 
		try { 
			log.debug("Persist zbus MQ data");
			FileOutputStream out = new FileOutputStream(DUMP_FILE);
			ObjectOutputStream mqOutputStream = new ObjectOutputStream(out); 
			mqOutputStream.writeObject(mqTable);
			mqOutputStream.close();
		} catch (FileNotFoundException e) { 
			e.printStackTrace();
		} catch (IOException e) { 
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public static ConcurrentMap<String, AbstractMQ> load(){
		ConcurrentMap<String, AbstractMQ> loaded = null;
		try {  
			FileInputStream in = new FileInputStream(DUMP_FILE);
			ObjectInputStream mqInputStream = new ObjectInputStream(in);
			loaded = (ConcurrentMap<String, AbstractMQ>) mqInputStream.readObject();
			mqInputStream.close();
		} catch (FileNotFoundException e) { 
			log.debug(e.getMessage(), e);
		} catch (IOException e) { 
			log.debug(e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			log.debug(e.getMessage(), e);
		}
		return loaded;
	}
	
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
