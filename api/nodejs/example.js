var util = require('util');
var events = require('events');
function MyStream(){
	events.EventEmitter.call(this);
}

util.inherits(MyStream, events.EventEmitter);
MyStream.prototype.write = function(data){
	this.emit("data", data);
}
var stream = new MyStream();
stream.on("data", function(data){
	console.log("recv: '"+data+"'");
});


stream.write("it works");