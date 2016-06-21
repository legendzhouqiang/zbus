var zbus = require("../zbus");
var Message = zbus.Message;
var MessageClient = zbus.MessageClient;
var Producer = zbus.Producer; 

var client = new MessageClient("127.0.0.1:15555");

client.connect(function(){ 
	var p = new Producer(client, "MyMQ");
    //p.createMQ( function(msg){});
	
    var msg = new Message();
    msg.setBody("hello world from node.js");
    
    p.send(msg, function(res){
        console.log(res.toString());
    }); 
   
});


