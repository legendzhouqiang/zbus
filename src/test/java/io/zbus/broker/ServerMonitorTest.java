package io.zbus.broker;

import io.zbus.mq.broker.ServerMonitor;
import io.zbus.mq.broker.ServerMonitor.ServerChangeHandler;
import io.zbus.net.EventDriver;

public class ServerMonitorTest { 
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		EventDriver driver = new EventDriver();
		ServerMonitor monitor = new ServerMonitor("127.0.0.1:15555", driver);
		
		monitor.setOnlineHandler(new ServerChangeHandler() { 
			@Override
			public void onServerChange(String serverAddress) {
				System.out.println(serverAddress + " online");
			}
		});
		
		monitor.setOfflineHandler(new ServerChangeHandler() {
			
			@Override
			public void onServerChange(String serverAddress) {
				System.out.println(serverAddress + " offline");
			}
		});
		
		
		monitor.start(); 
	} 
}
