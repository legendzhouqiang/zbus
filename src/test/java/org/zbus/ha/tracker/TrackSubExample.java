package org.zbus.ha.tracker;

import java.io.IOException;
import java.util.List;

import org.zbus.broker.ha.ServerEntry;
import org.zbus.broker.ha.TrackSub;
import org.zbus.broker.ha.TrackSub.PubServerEntryListHandler;
import org.zbus.broker.ha.TrackSub.ServerJoinHandler;
import org.zbus.broker.ha.TrackSub.ServerLeaveHandler;
import org.zbus.net.core.Dispatcher;

public class TrackSubExample {
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		Dispatcher dispatcher = new Dispatcher(); 
		String trackList = "127.0.0.1:16666;127.0.0.1:16667"; 
		
		TrackSub trackSub = new TrackSub(trackList, dispatcher);
			
		trackSub.onPubServerEntryList(new PubServerEntryListHandler() { 
			@Override
			public void onPubServerEntryList(List<ServerEntry> serverEntries) {
				for(ServerEntry se : serverEntries){
					System.out.println(se);
				}
			}
		});
		
		trackSub.onServerJoinHandler(new ServerJoinHandler() { 
			public void onServerJoin(String serverAddr) { 
				System.out.println("server joined: " + serverAddr); 
			}
		});
		
		trackSub.onServerLeaveHandler(new ServerLeaveHandler() { 
			public void onServerLeave(String serverAddr) throws IOException {
				System.out.println("server leave: " + serverAddr);
				
			}
		});
		
		trackSub.start(); 
	}
}
