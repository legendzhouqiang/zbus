package org.zbus.performance.net;

import java.io.IOException;

import org.zbus.kit.ConfigKit;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;
import org.zbus.performance.Perf;

public class NetPerf{
	
	public static void main(String[] args) throws Exception { 
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:8080");
		final int threadCount = ConfigKit.option(args, "-c", 16);  
		final int selectorCount = ConfigKit.option(args, "-selector", 0);  
		final int loopCount = ConfigKit.option(args, "-loop", 10000);    
		
		final SelectorGroup group = new SelectorGroup();
		group.selectorCount(selectorCount);
		
		Perf perf = new Perf() { 
			@Override
			public Task buildTask() { 
				final MessageClient client = new MessageClient(serverAddress, group);
				Task task = new Task() { 
					@Override
					public void doTask() throws Exception {
						Message msg = new Message();
						msg.setCmd("/hello");
						msg.setBody("hello world");
					    client.invokeSync(msg, 10000); 
					}
					
					@Override
					public void close() throws IOException {
						client.close();
					}
				};
				return task;
			}
			
			@Override
			public void close() throws IOException { 
				group.close(); 
			}
		};
		perf.threadCount = threadCount;
		perf.loopCount = loopCount;
		
		perf.run();
		
		perf.close();
	}
}
