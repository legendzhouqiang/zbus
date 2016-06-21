var zbus = require("../zbus");
var Message = zbus.Message;
var MessageClient = zbus.MessageClient;
var Consumer = zbus.Consumer;
var MqAdmin = zbus.MqAdmin;
var Mode = zbus.MqMode;


var client = new MessageClient("127.0.0.1:15555");
client.connect(function(){
	 var c = new Consumer(client, "MyPubSub");
     c.mode = Mode.PubSub; 
	 c.topic = 'sse';
	 
     c.recv(function(msg){
    	 console.log(msg.toString());
     });
});
