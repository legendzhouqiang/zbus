package io.zbus.mq;

import java.io.IOException;

import io.zbus.mq.Broker.ServerSelector;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.TopicInfo;
 

public class MqAdmin {      
	protected final Broker broker;
	protected ServerSelector adminServerSelector;
	
	protected String appid;
	protected String token; 
	protected int invokeTimeout = 10000;  // 10 s
	protected boolean verbose = false;
	
	public MqAdmin(MqConfig config){
		this.broker = config.getBroker();
		this.appid = config.getAppid();
		this.token = config.getToken();
		this.invokeTimeout = config.getInvokeTimeout();
		this.adminServerSelector = config.getAdminServerSelector();
		this.verbose = config.isVerbose();
		
		if(this.adminServerSelector == null){
			this.adminServerSelector = new DefaultAdminSelector();
		} 
	}
	
	protected MqClient configClient(MqClient client){
		client.setAppid(appid);
		client.setToken(token);
		client.setInvokeTimeout(invokeTimeout);
		return client;
	}
	
	public TopicInfo[] queryTopic(String topic) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		TopicInfo[] res = new TopicInfo[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try{ 
				client = pool.borrowClient();
				res[i] = configClient(client).queryTopic(topic);
			} catch (Exception e) { 
				res[i] = new TopicInfo();
				res[i].error = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res; 
	} 
	
	public TopicInfo[] declareTopic(String topic) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		TopicInfo[] res = new TopicInfo[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				res[i] = configClient(client).declareTopic(topic);
			} catch (Exception e) { 
				res[i] = new TopicInfo();
				res[i].error = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res; 
	} 
	
	public Object[] removeTopic(String topic) throws IOException, InterruptedException { 
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		Object[] res = new Object[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				configClient(client).removeTopic(topic);
			} catch (Exception e) { 
				res[i] = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res;  
	}  
	
	
	public Object[] emptyTopic(String topic) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		Object[] res = new Object[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				configClient(client).emptyTopic(topic);
			} catch (Exception e) { 
				res[i] = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res;  
	} 
	
	public Object[] pauseTopic(String topic) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		Object[] res = new Object[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				configClient(client).pauseTopic(topic);
			} catch (Exception e) { 
				res[i] = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res;  
	} 
	
	public Object[] resumeTopic(String topic) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		Object[] res = new Object[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				configClient(client).resumeTopic(topic);
			} catch (Exception e) { 
				res[i] = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res;  
	} 
	
	
	public ConsumeGroupInfo[] queryGroup(String topic, String group) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		ConsumeGroupInfo[] res = new ConsumeGroupInfo[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				res[i] = configClient(client).queryGroup(topic, group);
			} catch (Exception e) { 
				res[i] = new ConsumeGroupInfo();
				res[i].error = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res;  
	} 
	
	public ConsumeGroupInfo[] declareGroup(String topic, ConsumeGroup group) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		ConsumeGroupInfo[] res = new ConsumeGroupInfo[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				res[i] = configClient(client).declareGroup(topic, group);
			} catch (Exception e) { 
				res[i] = new ConsumeGroupInfo();
				res[i].error = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res;  
	} 
	
	public Object[] removeGroup(String topic, String group) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		Object[] res = new Object[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				configClient(client).removeGroup(topic, group);
			} catch (Exception e) { 
				res[i] = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res; 
	} 
	
	public Object[] emptyGroup(String topic, String group) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		Object[] res = new Object[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				configClient(client).emptyGroup(topic, group);
			} catch (Exception e) { 
				res[i] = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res; 
	} 
	
	public Object[] pauseGroup(String topic, String group) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		Object[] res = new Object[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				configClient(client).pauseGroup(topic, group);
			} catch (Exception e) { 
				res[i] = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res; 
	} 
	
	public Object[] resumeGroup(String topic, String group) throws IOException, InterruptedException {
		MqClientPool[] pools = broker.selectClient(this.adminServerSelector, topic);
		 
		Object[] res = new Object[pools.length];
		for(int i=0; i<pools.length; i++){
			MqClientPool pool = pools[i];
			MqClient client = null;
			try { 
				client = pool.borrowClient();
				configClient(client).resumeGroup(topic, group);
			} catch (Exception e) { 
				res[i] = e;
			} finally {
				pool.returnClient(client);
			}
		}
		return res; 
	}  
	
	public ServerSelector getAdminServerSelector() {
		return adminServerSelector;
	}

	public void setAdminServerSelector(ServerSelector adminServerSelector) {
		this.adminServerSelector = adminServerSelector;
	} 

	public static class DefaultAdminSelector implements ServerSelector{ 
		@Override
		public String[] select(BrokerRouteTable table, String topic) { 
			return table.serverMap().keySet().toArray(new String[0]); 
		} 
	}
}
