package org.zbus.examples.webservice;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

public class HelloWorldClient {
	
	public static void test(String address) throws Exception{
		URL url = new URL(address); 
		QName qname = new QName("http://webservice.examples.zbus.org/", "HelloWorldWSService"); 
		Service service = Service.create(url, qname); 
		
		HelloWorld hello = service.getPort(HelloWorld.class); 
		System.out.println(hello.getHelloWorldAsString("hong")); 
	}
	
	public static void main(String[] args) throws Exception { 
		String addressRaw = "http://localhost:8080/my-webservice/?wsdl";
		String addressZbus = "http://localhost:15555/my-webservice/?wsdl";
		test(addressRaw);
		test(addressZbus);
	} 
}
