var zbus = require("../zbus");
var Message = zbus.Message;
var MessageClient = zbus.MessageClient;
var Caller = zbus.Caller;


var client = new MessageClient("127.0.0.1:15555");
client.connect(function(){
    var caller = new Caller(client, "MyService");
    var msg = new Message();
    msg.setBody("hello world from node.js"); 
    
    caller.invoke(msg, function(res){
        console.log(res.toString());
    }); 
});


