package org.zbus.examples.webservice;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

//Service Implementation
@WebService(endpointInterface = "org.zbus.examples.webservice.HelloWorld")
public class HelloWorldWS implements HelloWorld {

	@Override
	public String getHelloWorldAsString(String name) {
		return "Hello World JAX-WS " + name;
	}
	
	

	public static void main(String[] args) {
		String url = "http://0.0.0.0:8080/";
		System.out.println("WebService @" +url);
		Endpoint.publish(url, new HelloWorldWS());
	}
}