# HttpDmzProxy for ZBUS

* [HttpDmzProxy]( http://git.oschina.net/rushmore/zbus/blob/master/src/main/java/org/zbus/proxy/HttpDmzProxy.java?dir=0&filepath=src%2Fmain%2Fjava%2Forg%2Fzbus%2Fproxy%2FHttpDmzProxy.java&oid=d21cab8096efcb6af2f2c0d936d93b6a5e163020&sha=eaa53da3a93d23bb8ef7293cd0411c90a0e7a2b0 "")

When talking about network restrains, there are some cases that we may not able to expose the service to the public, for example the service deployed in the server with only local IP address can not be access from public. 

Solutions always come to the proxy server, such as Nginx/Apache, If Nginx/Apache which deployed on a public server(with a public IP) is able to actively connect to the target server with only local IP, the target server can still be exposed to the public. However, when the target server can not be accessed, for instance service deployed on you server at home with only local IP via ADSL, what is the solution?

HttpDmzProxy works smoothly in the so called DMZ (DeMilitarized Zone) network environment. The fundamental restrain of DMZ network is:

	Program outside of DMZ CAN actively connect to DMZ, but 
	Program inside  of DMZ can NOT actively connect to the outside.

From the socket coding size of view, DMZ means the program in this zone only listens for socket in, while the program outside the DMZ only connect socket to the zone.


The HttpDmzProxy and zbus work smoothly for the network restrain of DMZ. 


	                ||          ||
	Browser  ====>  || zbus(MQ) ||  <==== HttpDmzProxy(Consumer) <====>[Target Server]
	                ||          ||

It functions just like Nginx/Apache in the user's perspective. The data flows in the following way:

	1) a URL prefix(to distinguish for different HTTP services) is added to original URL
	
	2) browser actively connect to zbus, the message received in zbus is handled as producing a message to the MQ,
	
	3) HttpDmzProxy consists of several consumers, actively connect to zbus to consume the message, and do the 
	really invocation to target server and route back the response message to the zbus, which finally choose the 
	right client(Browser).


So, the the core function implemented in the HttpDmzProxy is just doing right invocation as a consumer handler:

	
	public void handle(Message msg, final Consumer consumer) throws IOException {
		....
		String url = msg.getOriginUrl(); //zbus added this key-value, the target url in zbus way!!!
		...
		if (url.startsWith(prefix)) { //peel of the prefix to become the real one
			url = url.substring(prefix.length());
			if (!url.startsWith("/")) {
				url = "/" + url;
			}
		}
		...
		msg.setRequestString(url); // SET the real one
		msg.setServer(target); //set Target server 
		Message res = null;
		try {
			res = msg.invokeSimpleHttp(); //Do real target invocation
		} catch (IOException e) {
			....
		}
		....
		// route back message
		try {
			consumer.routeMessage(res);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
 
 Very simple? yes, it works just fine, try it!