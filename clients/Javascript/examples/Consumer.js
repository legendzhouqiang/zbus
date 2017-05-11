if (typeof module !== 'undefined' && module.exports) {
    var zbus = require("../../../src/main/resources/zbus.js"); 
    var Broker = zbus.Broker;
    var Consumer = zbus.Consumer;
}

var broker = new Broker();
broker.addTracker("localhost:15555");

var c = new Consumer(broker, "hong6"); 
c.onMessage = function (msg, client) {
	console.log(msg);
}
c.start();  
