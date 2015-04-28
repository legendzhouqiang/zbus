var zbus = require("../zbus");
var Message = zbus.Message;
var RemotingClient = zbus.RemotingClient;
var Rpc = zbus.Rpc;


var client = new RemotingClient("127.0.0.1:15555");
client.connect(function(){
    var rpc = new Rpc(client, "MyRpc");
    rpc.module = "Interface";
    rpc.invoke({
            method: "plus",
            params:[1,2]
        },
        function(res){ // res.result, or result.stackTrace
            console.log(res.result); 
        }
    );
});


