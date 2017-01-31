function Protocol(){ 
}
Protocol.VERSION_VALUE = "0.8.0";       //start from 0.8.0 

/////////////////////////Command Values/////////////////////////
//MQ Produce/Consume
Protocol.PRODUCE       = "produce";   
Protocol.CONSUME       = "consume";   
Protocol.ROUTE         = "route";     //route back message to sender, designed for RPC

//Topic control
Protocol.DECLARE_TOPIC = "declare_topic";  
Protocol.QUERY_TOPIC   = "query_topic"; 
Protocol.REMOVE_TOPIC  = "remove_topic";  
Protocol.PAUSE_TOPIC   = "pause_topic";  
Protocol.RESUME_TOPIC  = "resume_topic";  
Protocol.EMPTY_TOPIC   = "empty_topic";  

//ConsumerGroup control
Protocol.DECLARE_GROUP = "declare_group";  
Protocol.QUERY_GROUP   = "query_group"; 
Protocol.REMOVE_GROUP  = "remove_group";  
Protocol.PAUSE_GROUP   = "pause_group";  
Protocol.RESUME_GROUP  = "resume_group";  
Protocol.EMPTY_GROUP   = "empty_group";  
 

//High Availability (HA)
Protocol.TRACK_QUERY      = "track_query";  
Protocol.TRACK_PUB_TOPIC  = "track_pub_topic"; 
Protocol.TRACK_PUB_SERVER = "track_pub_server"; 
Protocol.TRACK_SUB        = "track_sub";   

//Simple HTTP server command
Protocol.PING          = "ping"; //ping server, returning back server time
Protocol.INFO          = "info"; //server info 
Protocol.VERSION       = "version";
Protocol.JS            = "js";   //serve javascript file
Protocol.CSS           = "css";  //serve css file 
Protocol.IMG           = "img";  //serve image file(SVG)
Protocol.PAGE          = "page";  //serve image file(SVG) 


/////////////////////////HTTP header extension/////////////////////////
//== Serialize/Deserialize
Protocol.COMMAND  = "cmd";     
Protocol.TOPIC    = "topic";
Protocol.FLAG     = "flag";
Protocol.TAG   	  = "tag";  
Protocol.OFFSET   = "offset";

Protocol.CONSUMER_GROUP       = "consumer_group";  
Protocol.CONSUME_BASE_GROUP   = "consume_base_group";  
Protocol.CONSUME_START_OFFSET = "consume_start_offset";
Protocol.CONSUME_START_MSGID  = "consume_start_msgid";
Protocol.CONSUME_START_TIME   = "consume_start_time";  
Protocol.CONSUME_WINDOW       = "consume_window";  
Protocol.CONSUME_FILTER_TAG   = "consume_filter_tag";   

Protocol.SENDER   = "sender"; 
Protocol.RECVER   = "recver";
Protocol.ID       = "id";	   
Protocol.SERVER   = "server";  
Protocol.ACK      = "ack";	  
Protocol.ENCODING = "encoding";
Protocol.DELAY    = "delay";
Protocol.TTL      = "ttl";  
Protocol.EXPIRE   = "expire"; 
Protocol.ORIGIN_ID     = "origin_id";   
Protocol.ORIGIN_URL    = "origin_url";  
Protocol.ORIGIN_STATUS = "origin_status";  

Protocol.APPID   = "appid";
Protocol.TOKEN   = "token";


/////////////////////////Flag values/////////////////////////
Protocol.FLAG_RPC    	     = 1<<0; 
Protocol.FLAG_EXCLUSIVE      = 1<<1;  
Protocol.FLAG_DELETE_ON_EXIT = 1<<2; 


var messageHandler = {
    get:function (obj, name, proxyed){
        if(obj[name] !== undefined)  
            return obj[name];        
        return "haha";  
    }
};

function Message(){  
	var msg = new Proxy(this, messageHandler);
	return msg;
}  

function hashSize(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
}

function uuid(){
    //http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
        return v.toString(16);
    });
}

function string2Buffer(str) {
  var buf = new ArrayBuffer(str.length*2); // 2 bytes for each char
  var bufView = new Int16Array(buf);
  for (var i=0, strLen=str.length; i<strLen; i++) {
    bufView[i] = str.charCodeAt(i);
  }
  return bufView;
}

function buffer2String(buf) {
  return String.fromCharCode.apply(null, buf);
}

//First, checks if it isn't implemented yet.
if (!String.prototype.format) {
  String.prototype.format = function() {
    var args = arguments;
    return this.replace(/{(\d+)}/g, function(match, number) { 
      return typeof args[number] != 'undefined'
        ? args[number]
        : match
      ;
    });
  };
}

function inherits(ctor, superCtor) {
	ctor.super_ = superCtor;
	ctor.prototype = Object.create(superCtor.prototype, {
		constructor : {
			value : ctor,
			enumerable : false,
			writable : true,
			configurable : true
		}
	});
};

var HttpStatus = {
    "200": "OK",
    "201": "Created",
    "202": "Accepted",
    "204": "No Content", 
    "206": "Partial Content", 
    "301": "Moved Permanently",
    "304": "Not Modified", 
    "400": "Bad Request", 
    "401": "Unauthorized", 
    "403": "Forbidden",
    "404": "Not Found", 
    "405": "Method Not Allowed", 
    "416": "Requested Range Not Satisfiable",
    "500": "Internal Server Error" 
};

function camel2underscore(key){
	return key.replace(/\.?([A-Z])/g, function (x,y){return "_" + y.toLowerCase()}).replace(/^_/, "");
}

function underscore2camel(key){
	return key.replace(/_([a-z])/g, function (g) { return g[1].toUpperCase(); });
}

function httpDump(msg){
	var lines = new Array();
	
	var body = msg.body;
	var bodyString = "\r\n";
	var bodyLen = 0;
	if(body){ 
		if(body instanceof Int16Array){
			body = buffer2String(body);
		} 
		bodyString += body;
		bodyLen = body.length;
	}
	msg['content-length'] = bodyLen;
	
	var nonHeaders = {'status': true, 'method': true, 'url': true, 'body': true}
	var meta = ""
	if(msg.status){
		var desc = HttpStatus[msg.status]; 
    	if(!desc) desc = "Unknown Status";
		meta = "HTTP/1.1 {0} {1}".format(msg.status, desc); 
	} else {
		var method = msg.method;
		if(!method) method = "GET";
		var url = msg.url;
		if(!url) url = "/";
		meta = "{0} {1} HTTP/1.1".format(method, url);  
	}
	lines.push(meta);
	for(var key in msg){
		if(key in nonHeaders) continue; 
		//replace camel style to underscore style
		key = camel2underscore(key);
		lines.push("{0}: {1}".format(key, msg[key])); 
	}   
	
	lines.push(bodyString);  
	return lines.join("\r\n");
};


//TODO handle when body is binary
function httpParse(str){
	var blocks = str.split("\r\n");
	var lines = [];
	for(var i in blocks){
		var line = blocks[i];
		if(line == '') continue;
		lines.push(line);
	}
	var lenKey = "content-length";
	var lenVal = 0;
	var msg = {};
	//parse first line 
	var bb = lines[0].split(" "); 
	if(bb[0].toUpperCase().startsWith("HTTP")){
		msg.status = bb[1];  
	} else {
		msg.method = bb[0];
		msg.url = bb[1];
	}
	
	for(var i=1;i<lines.length;i++){
		var line = lines[i];
		if(i == lines.length-1){
			if(lenVal > 0){
				msg.body = line;
				continue;
			}
		}
		
		var p = line.indexOf(":");
		if(p == -1) continue;
		var key = line.substring(0, p).trim().toLowerCase();
		var val = line.substring(p+1).trim(); 
		if(key == lenKey){
			lenVal = val;
			continue;
		} 
		key = underscore2camel(key);
		msg[key] = val;
	} 
	return msg;	 
}




