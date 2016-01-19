package org.zbus.mq;

public class DiskQuequeProducer {


	public static void main(String[] args) throws Exception { 
		DiskQueque.init("./test"); // 初始化MQ所在路径

		DiskQueque diskq = new DiskQueque("MyMQ");
		for(int i=0;i<10000000;i++){
			diskq.offer(new byte[1024]);
		}
		
		System.out.println(diskq.size());
		

		DiskQueque.destory(); // 清理掉Q环境
	}

}
