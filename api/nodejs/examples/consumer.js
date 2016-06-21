var zbus = require("../zbus");
var Message = zbus.Message;
var MessageClient = zbus.MessageClient;
var Consumer = zbus.Consumer;


var client = new MessageClient("127.0.0.1:15555");
client.connect(function(){
    var c = new Consumer(client, "MyMQ");
    c.recv(function(msg){
        console.log(msg.toString());
    });
});
