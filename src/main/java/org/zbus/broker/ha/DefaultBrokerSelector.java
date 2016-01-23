/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.broker.ha;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.zbus.broker.Broker;
import org.zbus.broker.Broker.BrokerHint;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.broker.ha.ServerEntryTable.ServerList;
import org.zbus.broker.ha.TrackSub.EntryRemoveHandler;
import org.zbus.broker.ha.TrackSub.EntryUpdateHandler;
import org.zbus.broker.ha.TrackSub.PubServerEntryListHandler;
import org.zbus.broker.ha.TrackSub.ServerJoinHandler;
import org.zbus.broker.ha.TrackSub.ServerLeaveHandler;
import org.zbus.kit.NetKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class DefaultBrokerSelector implements BrokerSelector{
	private static final Logger log = Logger.getLogger(DefaultBrokerSelector.class);
	private static final int localIpHashCode = Math.abs(NetKit.getLocalIp().hashCode());
	
	private final ServerEntryTable serverEntryTable = new ServerEntryTable();
	private final Map<String, Broker> allBrokers = new ConcurrentHashMap<String, Broker>();
	
	private TrackSub trackSub;
	
	private SelectorGroup selectorGroup = null;
	private boolean ownSelectorGroup = false;
	private BrokerConfig config;
	
	private CountDownLatch syncFromTracker = new CountDownLatch(1);
	private int syncFromTrackerTimeout = 3000;
	
	public DefaultBrokerSelector(BrokerConfig config){
		this.config = config;
		if(config.getSelectorGroup() == null){
			this.ownSelectorGroup = true;
			this.selectorGroup = new SelectorGroup();
			this.config.setSelectorGroup(selectorGroup);
		} else {
			this.selectorGroup = config.getSelectorGroup();
			this.ownSelectorGroup = false;
		}
		this.selectorGroup.start(); 
		
		subscribeNotification(); 
	}
	
	
	
	private Broker getBroker(String serverAddr){
		if(serverAddr == null) return null;
		return allBrokers.get(serverAddr);
	}
	 	
	private Broker getBrokerByIpCluster(){
		if(allBrokers.isEmpty()) return null;
		ArrayList<Broker> brokers = new ArrayList<Broker>(allBrokers.values());
		int idx = localIpHashCode%brokers.size();
		return brokers.get(idx);
	} 
	
	@Override
	public String getEntry(Message msg) {
		return msg.getMq();
	} 
	
	/**
	 * 优先级顺序:
	 * 1) 指定Server
	 * 2) 指定Entry
	 */
	public Broker selectByBrokerHint(BrokerHint hint) { 
		//1) Server最高优先级
		Broker broker = getBroker(hint.getServer()); //优先级最高
		if(broker != null) return broker;
		
		//2) Entry次之
		String entryId = hint.getEntry(); 
		if(entryId!= null){
			ServerList p = serverEntryTable.getServerList(entryId); 
			if(p != null && !p.isEmpty()){ 
				ServerEntry se = p.msgFirstList.get(0);
				if(se.unconsumedMsgCount > 0){ //存在未能消费掉消息的Entry，优先选择
					broker = getBroker(se.serverAddr);
					if(broker != null) return broker;
				}
			}
		} 
		
		//最后使用默认按IP簇集
		return getBrokerByIpCluster();
	} 
	
	
	@Override
	public List<Broker> selectByRequestMsg(Message msg) {  
		Broker broker = getBroker(msg.getServer());
		if(broker != null){
			return Arrays.asList(broker);
		}
		
		String entry = getEntry(msg);
		if(entry == null){
			broker = getBrokerByIpCluster();
			if(broker != null) return Arrays.asList(broker);
			return null;
		}
		
		ServerList serverList = serverEntryTable.getServerList(entry);
		if(serverList == null || serverList.isEmpty()){
			broker = getBrokerByIpCluster();
			if(broker != null) return Arrays.asList(broker);
			return null;
		} 

		int mode = serverList.getMode();
		if(ServerEntry.PubSub == mode){
			List<Broker> res = new ArrayList<Broker>();
			for(ServerEntry e : serverList){ 
				broker = getBroker(e.serverAddr);
				if(broker != null){
					res.add(broker);
				}
			}
			return res;
		} 
		
		int activeCount = 0;
		Iterator<ServerEntry> iter = serverList.consumerFirstList.iterator();
		while(iter.hasNext()){
			ServerEntry se = iter.next();
			if(se.consumerCount > 0) activeCount++; 
		}
		if(activeCount == 0){
			activeCount = serverList.consumerFirstList.size();
		} 
		if(activeCount == 0){
			return null;
		}
		
		int idx = localIpHashCode%activeCount; 
		ServerEntry se = serverList.consumerFirstList.get(idx);
		if(se != null){
			broker = getBroker(se.serverAddr);
			if(broker != null){
				return Arrays.asList(broker);
			} 
		} 
		return null;
	}

	@Override
	public Broker selectByClient(MessageClient client) { 
		String brokerAddr = client.attr("server"); 
		return allBrokers.get(brokerAddr); 
	}
	
	@Override
	public void close() throws IOException { 
		for(Broker broker : allBrokers.values()){
			broker.close();
		}
		allBrokers.clear();
		trackSub.close();
		serverEntryTable.close();
		
		if(ownSelectorGroup){
			selectorGroup.close();
		}
	}
	
	private void subscribeNotification(){
		trackSub = new TrackSub(config.getTrackServerList(), selectorGroup);
		
		trackSub.onServerJoinHandler(new ServerJoinHandler() {  
			public void onServerJoin(String serverAddr) throws IOException {
				if(serverEntryTable.isNewServer(serverAddr)){
					onNewServer(serverAddr); 
				}
			}
		});
		
		trackSub.onServerLeaveHandler(new ServerLeaveHandler() {  
			public void onServerLeave(String serverAddr) throws IOException {
				serverEntryTable.removeServer(serverAddr);
				synchronized (allBrokers) {
					Broker broker = allBrokers.remove(serverAddr);
					if(broker == null) return;
					log.info("Destroy broker to(%s)", serverAddr);
					broker.close(); 
				}
			}
		});
		
		trackSub.onEntryUpdateHandler(new EntryUpdateHandler() {  
			public void onEntryUpdate(ServerEntry entry) throws IOException { 
				updateServerEntry(entry);
			}
		});
		
		trackSub.onEntryRemoveHandler(new EntryRemoveHandler() {  
			public void onEntryRemove(String entryId, String serverAddr) throws IOException { 
				serverEntryTable.removeServerEntry(serverAddr, entryId);
			}
		});
		
		trackSub.onPubServerEntryList(new PubServerEntryListHandler() {  
			public void onPubServerEntryList(List<ServerEntry> serverEntries) throws IOException { 
				for(ServerEntry entry : serverEntries){ 
					updateServerEntry(entry); 
				}
				syncFromTracker.countDown();
			}
		});
		
		trackSub.start();
		
		try {
			syncFromTracker.await(syncFromTrackerTimeout, TimeUnit.MILLISECONDS);
			log.debug("Synchronized from Tracker");
		} catch (InterruptedException e) { 
			//ignore
		}
		if(syncFromTracker.getCount() > 0){
			log.debug("Timeout synchronizing from Tracker");
		}
	}
	
	private void updateServerEntry(ServerEntry entry) throws IOException{
		boolean isNewServer = serverEntryTable.isNewServer(entry); 
		serverEntryTable.updateServerEntry(entry); 
		if(isNewServer){
			onNewServer(entry.serverAddr);
		}
	}
	
	private void onNewServer(final String serverAddr) throws IOException{
		synchronized (allBrokers) {
			if(allBrokers.containsKey(serverAddr)) return;
			BrokerConfig brokerConfig = this.config.clone();
			brokerConfig.setServerAddress(serverAddr);
			Broker broker = new SingleBroker(brokerConfig);
			allBrokers.put(serverAddr, broker); 
		} 
	}
}