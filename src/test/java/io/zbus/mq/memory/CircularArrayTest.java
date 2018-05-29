package io.zbus.mq.memory;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.zbus.mq.model.Channel;
import io.zbus.mq.model.memory.CircularArray;
import io.zbus.mq.model.memory.MemoryChannelReader;

public class CircularArrayTest {
	@Test
	public void test() throws IOException {
		CircularArray q = new CircularArray();
		q.write("abc", "efg");
		q.write("xyz");

		MemoryChannelReader reader = new MemoryChannelReader(q, new Channel("MyChannel", 0L));
		assertEquals(3, reader.size());
		List<String> res = reader.read(20);
		assertEquals(res, Arrays.asList("abc", "efg", "xyz"));
		assertEquals(0, reader.size());
		
		reader.close();
	}
}
