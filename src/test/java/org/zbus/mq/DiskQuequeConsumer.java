package org.zbus.mq;

public class DiskQuequeConsumer {


	public static void main(String[] args) throws Exception {

		DiskQueque.init("c://test"); // 初始化MQ所在路径

		DiskQueque diskq = new DiskQueque("MyMQ");
		while(true){
			byte[] data = diskq.take();
			System.out.println(data);
		}
	}

}
