package io.zbus.rpc;


public interface Protocol {   
	public static final String STATUS    = "status";  // Response message status
	public static final String ID        = "id";      // Message ID
	public static final String BODY      = "body";    // Message body 
	public static final String API_KEY   = "apiKey";
	public static final String SIGNATURE = "signature";
	
	public static final String MODULE    = "module";
	public static final String METHOD    = "method";
	public static final String PARAMS    = "params";
}
