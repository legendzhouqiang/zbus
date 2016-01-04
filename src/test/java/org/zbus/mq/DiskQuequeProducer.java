package org.zbus.mq;

public class DiskQuequeProducer {


	public static void main(String[] args) throws Exception { 
		JvmDiskQueque.init("c://test"); // 初始化MQ所在路径

		JvmDiskQueque diskq = new JvmDiskQueque("MyMQ");
		diskq.offer(new byte[100]);
		

		JvmDiskQueque.destory(); // 清理掉Q环境
	}

}
