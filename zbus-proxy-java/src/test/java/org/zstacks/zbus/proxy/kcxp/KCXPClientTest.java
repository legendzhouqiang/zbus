package org.zstacks.zbus.proxy.kcxp;

import java.util.HashMap;
import java.util.Map;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.proxy.kcxp.KCXPClient;
import org.zstacks.zbus.proxy.kcxp.KCXPResult;

public class KCXPClientTest {

	public static void main(String[] args) throws Exception {
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);

		KCXPClient kcxp = new KCXPClient(broker, "KCXP"); 

		String funcId = "L0063001"; 
		
		Map<String, String> params = new HashMap<String, String>(); 
		params.put("g_operpwd", "");
		params.put("OPER_CODE", "");
		params.put("BROKER_CODE", "110050000012");
		params.put("ORG_TYPE", "0");
		params.put("OP_USER", "8888");
		params.put("g_operway", "g");
		params.put("F_OP_SITE", "172.24.174.52");
		params.put("g_operid", "8888");
		params.put("CUST_CODE", "");
		params.put("F_FUNCTION", "");
		params.put("F_SUBSYS", "1");
		params.put("F_CHANNEL", "g");
		params.put("INT_ORG", "1100");
		params.put("CUACCT_CODE", ""); 
		params.put("F_RUNTIME", "ZKrp98YaBUbHTiGiu+6GPS8XhxLWygjS");
		params.put("F_SESSION", "010200000000000022B810072210072302311000000000027cONBmMR/ks=AF7YwUJQwUaUvkRqR7Xl7H9C5AUMYrCHjfaLdrPspF4="); 
		params.put("F_OP_USER", "8888");
		params.put("F_OP_ROLE", "2"); 
		params.put("ORG_CODE", "1100");
		params.put("g_checksno", ""); 
		params.put("RIGHT_FLAG", "");
		params.put("g_serverid", "1"); 
		params.put("g_stationaddr", ""); 

		for(int i=0;i<1;i++){
			KCXPResult res = kcxp.request(funcId, params);
			res.dump();
		}
		
		

	}
}
