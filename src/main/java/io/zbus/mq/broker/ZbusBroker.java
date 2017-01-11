package io.zbus.mq.broker;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.net.Sync.ResultCallback;
import io.zbus.net.http.Message;
import io.zbus.net.http.Message.MessageInvoker;

/**
 * Broker factory class, abstraction of all broker types
 * 1) JvmBroker, brokerAddess=null/jvm 
 * 2) SingleBroker, brokerAddress=ip:port, eg. 127.0.0.1:15555
 * 3) HaBroker, brokerAddress=[ip:port;ip:port], 
 * 	 '[' and ']' could be omitted, ';' ',' and ' ' are supported to split trackServer ip:port list
 *   eg. [127.0.0.1:16666;127.0.0.1:166667], [127.0.0.1:16666]
 *   127.0.0.1:16666;127.0.0.1:16667
 *   
 * @author rushmore (洪磊明)
 *
 */
public class ZbusBroker implements Broker{
	private Broker support;
	
	/**
	 * Default to SingleBroker to localhost:15555
	 * @throws IOException if underlying IO exception occurs
	 */
	public ZbusBroker() throws IOException { 
		this(new BrokerConfig());
	}
	
	public ZbusBroker(String brokerAddress) throws IOException {
		this(new BrokerConfig(brokerAddress));
	}
	
	/**
	 * Build underlying Broker by borkerAddress
	 * @param config
	 * @throws IOException  if underlying IO exception occurs
	 */
	public ZbusBroker(BrokerConfig config) throws IOException { 
		String brokerAddress = config.getBrokerAddress();  
		if(brokerAddress == null || "jvm".equalsIgnoreCase(brokerAddress.trim())){
			if(config.getMqServer() != null){
				support = new JvmBroker(config.getMqServer());
			} else {
				if(config.getMqServerConfig() != null){
					if(config.getMqServerConfig().getEventDriver() == null){
						config.getMqServerConfig().setEventDriver(config.getEventDriver());
					} 
					support = new JvmBroker(config.getMqServerConfig());
				} else {
					support = new JvmBroker();
				}
			}
			return;
		} 
		brokerAddress = brokerAddress.trim();
		boolean ha = false;
		if(brokerAddress.startsWith("[")){
			if(brokerAddress.endsWith("]")){
				brokerAddress = brokerAddress.substring(1, brokerAddress.length()-1);
				ha = true;
			} else {
				throw new IllegalArgumentException(brokerAddress + " broker address invalid");
			}
		}  
		if(brokerAddress.contains(",") || brokerAddress.contains(" ") || brokerAddress.contains(";")){
			ha = true;
		} 
		config.setBrokerAddress(brokerAddress);
		if(ha){
			support = new HaBroker(config);  
		} else {
			support = new SingleBroker(config);
		}
	}
	
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException, InterruptedException {
		return support.invokeSync(req, timeout);
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback) throws IOException {
		support.invokeAsync(req, callback);
	}

	@Override
	public void close() throws IOException {
		support.close();
	}

	@Override
	public MessageInvoker getInvoker(BrokerHint hint) throws IOException {
		return support.getInvoker(hint);
	}

	@Override
	public void closeInvoker(MessageInvoker client) throws IOException {
		support.closeInvoker(client);
	}

	@Override
	public Message invokeSync(Message req) throws IOException, InterruptedException {
		return support.invokeSync(req);
	}

}
