var zbus = require("../zbus");
var Message = zbus.Message;
var RemotingClient = zbus.RemotingClient;
var Consumer = zbus.Consumer;


var client = new RemotingClient("127.0.0.1:15555");
client.connect(function(){
    var consumer = new Consumer(client, "MyMQ");
    consumer.recv(function(msg){
        console.log(msg.toString());
    });
});
