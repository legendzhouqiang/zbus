#Great many thanks to zookeeper project.
##zookeeper version: 3.4.8, 

Simple modifications in zbus project to simplify dependencies:
* Log dependency removed to rely on zbus's logger adaptor 
* Jline dependency removed 
* Jute moved to zookeeper package
* QuorumPeerMain default to read conf/zbus-zk.cfg 
* Upgrade netty dependency version from 3.7 to 4.0
