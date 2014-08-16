package org.zbus.server.mq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.zbus.common.ConsumerInfo;
import org.zbus.remoting.Message;
import org.zbus.remoting.znet.Session;

public class MQ extends AbstractMQ {   
	private static final long serialVersionUID = -1610052009303680593L;
	
	final BlockingQueue<PullSession> sessQ = new LinkedBlockingQueue<PullSession>();
	
	public MQ(String name, ExecutorService executor, int mode){
		super(name, executor, mode); 
	}  
	
	public void consume(Message msg, Session sess) throws IOException{ 
		for(PullSession pull : sessQ){
			if(pull.getSession() == sess){
				pull.setPullMsg(msg);
				return; 
			}
		} 
		PullSession pull = new PullSession(sess, msg);
		sessQ.offer(pull);  
		this.dispatch();
	} 
	
	public void cleanSession(){
		Iterator<PullSession> iter = sessQ.iterator();
		while(iter.hasNext()){
			PullSession ps = iter.next();
			if(!ps.session.isActive()){
				iter.remove();
			}
		}
	}
	
	@Override
	void doDispatch() throws IOException{ 
		while(msgQ.peek() != null && sessQ.peek() != null){
			PullSession pull = sessQ.poll(); 
			if(pull == null){
				continue;
			}
			if( !pull.getSession().isActive() ){ 
				continue;
			} 
			
			Message msg = msgQ.poll();
			if(msg == null){
				continue;
			}
			
			try { 
				String status = msg.removeHead(Message.HEADER_REPLY_CODE);
				if(status == null){
					status = "200";
				} 
				
				Message pullMsg = pull.getPullMsg();
				msg.setStatus(status);
				msg.setMsgIdRaw(msg.getMsgId());  //保留原始消息ID
				msg.setMsgId(pullMsg.getMsgId()); //配对订阅消息！
				pull.getSession().write(msg);
			} catch (IOException ex) {  
				msgQ.offer(msg); //TODO 消息顺序处理
			} 
		} 
	}
	
	public List<ConsumerInfo> getConsumerInfoList() {
		List<ConsumerInfo> res = new ArrayList<ConsumerInfo>();
		
		Iterator<PullSession> it = sessQ.iterator();
		while(it.hasNext()){
			PullSession value = it.next();
			Session sess = value.getSession(); 
			ConsumerInfo info = new ConsumerInfo();
			info.setStatus(sess.getStatus().toString());
			info.setRemoteAddr(sess.getRemoteAddress());
			res.add(info);
		}
		return res;
	}
	 
}
