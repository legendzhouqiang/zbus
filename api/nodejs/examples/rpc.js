var zbus = require("../zbus");
var Message = zbus.Message;
var RemotingClient = zbus.RemotingClient;
var Rpc = zbus.Rpc;


var client = new RemotingClient("127.0.0.1:15555");
client.connect(function(){
    var rpc = new Rpc(client, "MyRpc");
    var msg = new Message();
    msg.setBody("hello world from node.js");
    rpc.invoke(msg, function(res){
        console.log(res.toString());
    });
});


