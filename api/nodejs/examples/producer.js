var zbus = require("../zbus");
var Message = zbus.Message;
var RemotingClient = zbus.RemotingClient;
var Producer = zbus.Producer;


var client = new RemotingClient("127.0.0.1:15555");
client.connect(function(){
    var producer = new Producer(client, "MyMQ");
    var msg = new Message();
    msg.setBody("hello world from node.js");
    producer.send(msg, function(res){
        console.log(res.toString());
    });
});


