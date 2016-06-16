#Great many thanks to zookeeper project.
##zookeeper version: 3.4.8, 

tiny modified in zbus project to simplify dependencies:
* log dependency removed, rely on zbus's logger adaptor 
* jline dependency removed 
* jute moved to zookeeper package
* QuorumPeerMain default to read conf/zbus-zk.cfg 
