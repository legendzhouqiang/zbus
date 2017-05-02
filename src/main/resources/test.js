var zbus = require('./zbus.js');


var client = new zbus.MessageClient({address: 'localhost:15555', sslEnabled: false});
client.connect(function(){
	console.log("connected"); 
});