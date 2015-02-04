package org.zbus.server.mq.store;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.zbus.common.MessageMode;
import org.zbus.common.MqInfo;
import org.zbus.common.json.JSON;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.Message;
import org.zbus.remoting.MessageCodec;
import org.zbus.remoting.nio.IoBuffer;
import org.zbus.server.mq.MessageQueue;
import org.zbus.server.mq.RequestQueue;

public class MessageStoreSql implements MessageStore {
	private static final Logger log = LoggerFactory.getLogger(MessageStoreSql.class);
	private static final MessageCodec codec = new MessageCodec();
	
	Connection connection; 
	public MessageStoreSql() {  
	}
	
	private Connection getConnection(){
		try { 
			return DriverManager.getConnection("jdbc:hsqldb:db/zbus", "sa", "");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}
	
	public void start(){
		try {
			Class.forName("org.hsqldb.jdbcDriver");
			connection = getConnection();
			this.initDbTable();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public void shutdown() {
		if(connection == null) return;
		try{
			Statement st = connection.createStatement();
			st.execute("SHUTDOWN");
			connection.close();
		}catch(Exception e){
			log.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void saveMessage(Message msg) {
		try{
			String msgId = msg.getMsgId();
			this.update("INSERT INTO msgs(id, msg_str) VALUES(?,?)",
					msgId, msg.toString());
			this.update("INSERT INTO mq_msgs(mq_id, msg_id) VALUES(?,?)", 
					msg.getMq(), msgId);
			log.info("save " + msgId);
		} catch(Exception e){
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void removeMessage(Message msg) { 
		try{
			String msgId = msg.getMsgId();
			this.update("DELETE FROM msgs WHERE id=?", msgId);
			this.update("DELETE FROM mq_msgs WHERE msg_id=?", msgId);
			log.info("delete " + msgId);
		} catch(Exception e){
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void onMessageQueueCreated(MessageQueue mq) {
		try{
			String json = JSON.toJSONString(mq.getMqInfo());
			this.update("INSERT INTO mqs(id, mq_info) VALUES(?,?)",
					mq.getName(), json); 
		} catch(Exception e){
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void onMessageQueueRemoved(MessageQueue mq) {
		try{
			this.update("DELETE FROM mqs WHERE id=?", mq.getName()); 
		} catch(Exception e){
			log.error(e.getMessage(), e);
		}
	}

	
	@Override
	public ConcurrentMap<String, MessageQueue> loadMqTable() throws SQLException {
		ConcurrentHashMap<String, MessageQueue> res = new ConcurrentHashMap<String, MessageQueue>();
		ResultSet mqRs = this.query("SELECT * FROM mqs");
		while(mqRs.next()) {
			String mqName = mqRs.getString("id");
			String mqInfoString = mqRs.getString("mq_info");  
			MqInfo info = JSON.parseObject(mqInfoString, MqInfo.class);
			int mode = info.getMode();
			if(!MessageMode.isEnabled(mode, MessageMode.MQ)){
				log.warn("message queue mode not support");
				continue;
			} 

			RequestQueue mq = new RequestQueue(info.getBroker(),
					mqName, null, mode); 
			mq.setCreator(info.getCreator());
			
			String sql = String.format("SELECT msg_str FROM mq_msgs, msgs WHERE mq_msgs.msg_id = msgs.id AND mq_msgs.mq_id='%s'", mqName);
			ResultSet msgRs = this.query(sql);
			List<Message> msgs = new ArrayList<Message>();
			while(msgRs.next()){
				String msgString = msgRs.getString("msg_str");
				IoBuffer buf = IoBuffer.wrap(msgString);
				Message msg = (Message) codec.decode(buf);
				if(msg != null){
					msgs.add(msg);
				} else {
					log.error("message decode error");
				}
			}
			msgRs.close();
			mq.loadMessageList(msgs);
			res.put(mqName, mq);
		}
		mqRs.close();
		return res;
	}
	

	private void initDbTable() throws SQLException{
		this.update("CREATE TABLE IF NOT EXISTS msgs(id VARCHAR(128), msg_str VARCHAR(102400000), PRIMARY KEY(id) )");
		this.update("CREATE TABLE IF NOT EXISTS mq_msgs(mq_id VARCHAR(128), msg_id VARCHAR(128) )");
		this.update("CREATE TABLE IF NOT EXISTS mqs(id VARCHAR(1024), mq_info VARCHAR(102400000), PRIMARY KEY(id) )");
	}
	
	private synchronized ResultSet query(String sql) throws SQLException {
		if(this.connection == null) return null;
		Statement st = connection.createStatement();
		return st.executeQuery(sql); 
	}
	
	private synchronized void update(String sql, Object... args) throws SQLException { 
		if(this.connection == null) return;
		PreparedStatement st = connection.prepareStatement(sql);
		for(int i=0;i<args.length;i++){
			st.setObject(i+1, args[i]);
		}
		int i = st.executeUpdate();
		if (i == -1) {
			log.error("db error : " + sql);
		}
		st.close();
	}
	

}
