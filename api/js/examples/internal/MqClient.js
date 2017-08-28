if (typeof module !== 'undefined' && module.exports) {
    var zbus = require("../../zbus.js");
	MqClient = zbus.MqClient; 
	var logger = zbus.logger;
}  
logger.level = logger.DEBUG; //from zbus.js

async function example(){
 
var client = new MqClient("localhost:15555");   
await client.connect();


await client.declare('MyTopic'); 
 
res = await client.query('MyTopic');
console.log(res);

await client.produce({mq: 'MyTopic', body: 'hello from js async/await'});
var msg = await client.consume({mq: 'MyTopic'});
console.log(msg.body);

client.close();
} 
example();