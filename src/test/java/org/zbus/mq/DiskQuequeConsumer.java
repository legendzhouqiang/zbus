package org.zbus.mq;

public class DiskQuequeConsumer {


	public static void main(String[] args) throws Exception {

		DiskQueque.init("./test"); // 初始化MQ所在路径

		DiskQueque diskq = new DiskQueque("MyMQ");
		int i = 0;
		while(true){
			byte[] data = diskq.take();
			if(data == null) break;
			i++;
			if(i%10000 == 0){
				System.out.println(i);
			}
		}
	}

}
