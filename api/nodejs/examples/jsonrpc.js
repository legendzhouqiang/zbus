var zbus = require("../zbus");
var Message = zbus.Message;
var RemotingClient = zbus.RemotingClient;
var JsonRpc = zbus.JsonRpc;


var client = new RemotingClient("127.0.0.1:15555");
client.connect(function(){
    var jsonRpc = new JsonRpc(client, "MyJsonRpc");
    jsonRpc.module = "ServiceInterface";
    jsonRpc.invoke({
            method: "plus",
            params:[1,2]
        },
        function(res){
            console.log(res.toString());
    });
});


