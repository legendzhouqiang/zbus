var zbus = require("../zbus");
var Message = zbus.Message;
var MessageClient = zbus.MessageClient;
var Producer = zbus.Producer; 

var client = new MessageClient("127.0.0.1:15555");

client.connect(function(){
    var pub = new Producer(client, "MyPubSub");
    var msg = new Message();
	msg.setTopic('sse');
    msg.setBody("hello world from node.js");
    
    pub.send(msg, function(res){
        console.log(res.toString()); 
    });
});


