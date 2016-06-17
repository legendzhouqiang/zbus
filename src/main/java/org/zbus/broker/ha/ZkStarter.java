package org.zbus.broker.ha;

import org.zbus.kit.ConfigKit;
import org.zbus.zookeeper.server.quorum.QuorumPeerMain;

public class ZkStarter {

	public static void main(String[] args) throws Exception { 
		String xmlConfigFile = ConfigKit.option(args, "-conf", "conf/zbus.xml");
		
		QuorumPeerMain.runFromXmlNode(xmlConfigFile, "//zookeeper");
	}
}
