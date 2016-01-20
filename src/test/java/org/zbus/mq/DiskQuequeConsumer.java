package org.zbus.mq;

public class DiskQuequeConsumer {


	public static void main(String[] args) throws Exception {

		DiskQueue.init("./test"); // 初始化MQ所在路径

		DiskQueue diskq = new DiskQueue("MyMQ");
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
