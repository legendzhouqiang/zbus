package org.zbus.mq;

public class DiskQuequeConsumer {


	public static void main(String[] args) throws Exception {

		JvmDiskQueque.init("c://test"); // 初始化MQ所在路径

		JvmDiskQueque diskq = new JvmDiskQueque("MyMQ");
		while(true){
			byte[] data = diskq.take();
			System.out.println(data);
		}
	}

}
