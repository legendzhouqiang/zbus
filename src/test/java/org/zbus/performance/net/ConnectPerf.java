package org.zbus.performance.net;

import java.io.IOException;

import org.zbus.kit.ConfigKit;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.MessageClient;
import org.zbus.performance.Perf;

public class ConnectPerf {

	public static void main(String[] args) throws Exception {
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigKit.option(args, "-c", 16);
		final int selectorCount = ConfigKit.option(args, "-selector", 0);
		final int loopCount = ConfigKit.option(args, "-loop", 10000);

		final SelectorGroup group = new SelectorGroup();
		group.selectorCount(selectorCount);

		Perf perf = new Perf() {
			@Override
			public TaskInThread buildTaskInThread() {
				TaskInThread task = new TaskInThread() {
					@Override
					public void doTask() throws Exception { 
						MessageClient client = new MessageClient(serverAddress, group);
						client.connectSync();
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
		perf.logInterval = 10000;

		perf.run();

		perf.close();
	}
}
