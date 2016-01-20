package org.zbus.mq;

public class DiskQuequeProducer {


	public static void main(String[] args) throws Exception { 
		DiskQueue.init("./test"); // 初始化MQ所在路径

		DiskQueue diskq = new DiskQueue("MyMQ");
		for(int i=0;i<10000000;i++){
			diskq.offer(new byte[1024]);
		}
		
		System.out.println(diskq.size());
		

		DiskQueue.release(); // 清理掉Q环境
	}

}
