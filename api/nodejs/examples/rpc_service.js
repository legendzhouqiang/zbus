var zbus = require("../zbus");
var Message = zbus.Message;
var RemotingClient = zbus.RemotingClient;
var RpcService = zbus.RpcService;


var client = new RemotingClient("127.0.0.1:15555");
client.connect(function(){
    var service = new RpcService(client, "MyRpc");
    service.serve(function(msg){
        console.log(msg.toString());
        var res = new Message();
        res.setBodyFormat("reply from node.js service, {0}", new Date());
        return res;
    });
});


