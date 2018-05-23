package io.zbus.mq.memory;

import java.util.List;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.memory.CircularArray;
import io.zbus.mq.model.memory.CircularArray.Reader;

public class CircularArrayTest {
	public static void main(String[] args) {
		CircularArray q = new CircularArray();
		q.write("abc", "efg");
		q.write("xyz");

		Reader reader = q.createReader(new Channel("MyChannel", 0L));
		System.out.println(reader.size());
		List<String> res = reader.read(20);
		for (String s : res) {
			System.out.println(s);
		}
		System.out.println(reader.size());
	}
}
