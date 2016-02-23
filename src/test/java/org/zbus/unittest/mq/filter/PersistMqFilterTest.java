package org.zbus.unittest.mq.filter;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zbus.kit.FileKit;
import org.zbus.mq.server.filter.MqFilter;
import org.zbus.mq.server.filter.PersistMqFilter;

public class PersistMqFilterTest {
	private MqFilter filter;
	private String dbPath = "./unitest_db";
	@Before
	public void setUp() throws Exception {
		filter = new PersistMqFilter(dbPath);
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
		FileKit.deleteFile(new File(dbPath));
	}

	@Test
	public void test() {
		assertTrue(filter.addKey(null, null, null) == 0);
		assertTrue(filter.addKey("", null, null) == 0);
		
		assertTrue(filter.removeKey(null, null, null) == 0);
		assertTrue(filter.removeKey("", null, null) == 0);
		
		assertTrue(filter.addKey("", null, "xxx") == 1);
		assertTrue(filter.addKey("", "test", "xxx") == 1);
		
		final int n = 10000;
		for(int i=0;i<n;i++){
			filter.addKey("MyMQ", null, ""+i);
		}
		int m = filter.removeKey("MyMQ", null, null);
		assertTrue(m == n);
		
		int m1 = filter.addKey("MyMQ", "group1", "hello");
		int m2 = filter.removeKey("MyMQ", "group1", "hello");
		assertTrue(m1 == 1);
		assertTrue(m2 == 1);
	}

}
