var http = require('http');

var data = {
	'method': 'getString',
	'params': ['test']
}
var body = JSON.stringify(data);

var options = {
	hostname: 'localhost',
	port: 15555,
	path: '/',
	method: 'POST',
	headers: {
		'mq': 'MyRpc',
		'cmd': 'produce',
		'ack': false,
		
		'content-length': body.length
	}
};

var req = http.request(options, function(feedback){
	if (feedback.statusCode == 200) {  
        var body = "";  
        feedback.on('data', function (data) { 
        	console.log(new String(data)); 
        })  
    }   
});

req.write(body);
req.end();
