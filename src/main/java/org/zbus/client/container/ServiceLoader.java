package org.zbus.client.container;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.zbus.client.Broker;
import org.zbus.client.Service;
import org.zbus.client.ServiceConfig;

public class ServiceLoader {
	private Broker broker;
	private ExecutorService executor = new ThreadPoolExecutor(4, 16, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	
	public ServiceLoader(Broker broker){
		this.broker = broker;
	}
	
	public void loadService(final ServiceProvider provider){
		executor.execute(new Runnable() {
			@Override
			public void run() { 
				ServiceConfig config = provider.getConfig();
				if(config.getBroker() == null){
					config.setBroker(broker);
				}
				Service service = new Service(config);
				service.start();
			}
		});
	}
	
	public void loadService(List<ServiceProvider> providers){
		for(ServiceProvider p : providers){
			loadService(p);
		}
	}
}
