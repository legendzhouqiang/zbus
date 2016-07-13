package org.zbus.examples.httpclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.client.methods.CloseableHttpResponse;

public class HttpKit {
	public static void printResponse(CloseableHttpResponse resp) throws Exception{
		BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
		while (true) {
			String line = reader.readLine();
			if(line == null) break;
			System.out.println(line);
		}
		reader.close();
	}
}
