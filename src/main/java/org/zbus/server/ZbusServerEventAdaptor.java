package org.zbus.server;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import org.zbus.common.MessageMode; 
import org.zbus.remoting.Message;
import org.zbus.remoting.MessageHandler;
import org.zbus.remoting.ServerEventAdaptor;
import org.zbus.remoting.znet.Session;
import org.zbus.server.mq.AbstractMQ;

public class ZbusServerEventAdaptor extends ServerEventAdaptor{ 
	private ConcurrentMap<String, AbstractMQ> mqTable = null;
	
    @Override
    public void onMessage(Object obj, Session sess) throws IOException {  
    	
    	Message msg = (Message)obj;  
    	if(this.globalHandler != null){
    		this.globalHandler.handleMessage(msg, sess);
    	}
    	
    	String cmd = msg.getCommand();
    	if(cmd == null){ 
    		Message res = new Message();
    		res.setMsgId(msg.getMsgId()); 
        	res.setStatus("400");
        	res.setBody("Bad format: missing command"); 
        	sess.write(res);
    		return;
    	}
    	
    	MessageHandler handler = handlerMap.get(cmd);
    	if(handler != null){
    		handler.handleMessage(msg, sess);
    		return;
    	}
    	
    	Message res = new Message();
    	res.setMsgId(msg.getMsgId()); 
    	res.setStatus("400");
    	String text = String.format("Bad format: command(%s) not support", cmd);
    	res.setBody(text); 
    	sess.write(res); 
    } 
  
    @Override
    public void onException(Throwable e, Session sess) throws IOException {
    	if(! (e instanceof IOException) ){
			super.onException(e, sess);
		}
    	this.cleanMQ(sess);
    }
    
    @Override
    public void onSessionDestroyed(Session sess) throws IOException {  
    	this.cleanMQ(sess);
    }
    
    
    private void cleanMQ(Session sess){
    	if(this.mqTable == null) return;
    	String creator = sess.getRemoteAddress();
    	Iterator<Entry<String, AbstractMQ>> iter = this.mqTable.entrySet().iterator();
    	while(iter.hasNext()){
    		Entry<String, AbstractMQ> e = iter.next();
    		AbstractMQ mq = e.getValue();
    		if(MessageMode.isEnabled(mq.getMode(), MessageMode.Temp)){
    			if(mq.getCreator().equals(creator)){
        			iter.remove();
        		}
    		} 
    	}
    }

    
	public void setMqTable(ConcurrentMap<String, AbstractMQ> mqTable) {
		this.mqTable = mqTable;
	} 
    
}

