package org.zbus.server.mq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.Message;
import org.zbus.remoting.nio.Session;
import org.zbus.server.mq.info.ConsumerInfo;

public class RequestQueue extends MessageQueue {   
	private static final Logger log = LoggerFactory.getLogger(RequestQueue.class);
	private static final long serialVersionUID = -1610052009303680593L;
	
	protected final BlockingQueue<Message> msgQ = new LinkedBlockingQueue<Message>();
	transient BlockingQueue<PullSession> sessQ = new LinkedBlockingQueue<PullSession>();
	
	public RequestQueue(String name, ExecutorService executor, int mode){
		super(name, executor, mode); 
	}  
	
	public void produce(Message msg, Session sess) throws IOException{
		String msgId = msg.getMsgId(); 
		if(msg.isAck()){
			ReplyHelper.reply200(msgId, sess);
		} 
		
    	msgQ.offer(msg);  
    	this.dispatch();
	}
	
	public void consume(Message msg, Session sess) throws IOException{ 
		for(PullSession pull : sessQ){
			if(pull.getSession() == sess){
				pull.setPullMsg(msg);
				this.dispatch();
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
			if(pull == null || pull.window.get() == 0){
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
				msg.setMsgIdSrc(msg.getMsgId());  //保留原始消息ID
				msg.setMsgId(pullMsg.getMsgId()); //配对订阅消息！
				pull.getSession().write(msg); 
				
				if(pull.window.get()>0){
					pull.window.decrementAndGet();
				}
				
			} catch (IOException ex) {   
				log.error(ex.getMessage(), ex); 
				msgQ.offer(msg);
			} 

			if(pull.window.get() == -1 || pull.window.get() > 0){
				sessQ.offer(pull);
			}
		} 
	}
	 
	//used when load from dump
	public void restoreFromDump(ExecutorService executor) {
		this.executor = executor;
		this.sessQ = new LinkedBlockingQueue<PullSession>();
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
	@Override
	public int getMessageQueueSize() {
		return this.msgQ.size();
	}
	 
}
