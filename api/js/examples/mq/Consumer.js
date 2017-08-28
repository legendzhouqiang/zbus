﻿if (typeof module !== 'undefined' && module.exports) {
    var zbus = require("../../zbus.js"); 
    Broker = zbus.Broker;
    Consumer = zbus.Consumer;
	var logger = zbus.logger;
}  
logger.level = logger.DEBUG; //from zbus.js

var broker = new Broker("localhost:15555"); 

var c = new Consumer(broker, "MyTopic");
  
c.messageHandler = function (msg, client) {
    logger.info(msg.body);
}
c.start(); 

