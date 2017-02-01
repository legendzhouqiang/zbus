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

/** 
 * msg.encoding = "utf8"; //encoding on message body
 * msg.body can be 
 * 1) ArrayBuffer, Uint8Array, Init8Array to binary data
 * 2) string value
 * 3) otherwise, JSON converted inside
 * 
 * @param msg
 * @returns ArrayBuffer
 */
function httpDump(msg){
	var headers = "";
	var encoding = msg.encoding;
	if(!encoding) encoding = "utf8";
	var encoder = new TextEncoder(encoding);
	
	var body = msg.body;  
	var contentType = "application/octet-stream";
	if (body) { 
		if(body instanceof ArrayBuffer){
			body = new Uint8Array(body);
		} else if(body instanceof Uint8Array || body instanceof Int8Array){
			//no need to handel
		} else { 
			if (typeof body != 'string') {
				body = JSON.stringify(body);
				contentType = "application/json";
			} else {
				contentType = "text/html";
			} 
			body = encoder.encode(body); 
		}  
	} 
	msg["content-length"] = body.byteLength;
	msg["content-type"] = contentType;
	
	var nonHeaders = {'status': true, 'method': true, 'url': true, 'body': true}
	var line = ""
	if(msg.status){
		var desc = HttpStatus[msg.status]; 
    	if(!desc) desc = "Unknown Status";
		line = "HTTP/1.1 {0} {1}\r\n".format(msg.status, desc); 
	} else {
		var method = msg.method;
		if(!method) method = "GET";
		var url = msg.url;
		if(!url) url = "/";
		line = "{0} {1} HTTP/1.1\r\n".format(method, url);  
	}
	
	
	headers += line; 


	for(var key in msg){
		if(key in nonHeaders) continue;  
		line = "{0}: {1}\r\n".format(camel2underscore(key), msg[key]);
		headers += line;
	}
	headers += "\r\n";
	delete msg["content-length"]; //clear
	delete msg["content-type"]
     
	var headerBuffer = encoder.encode(headers); 
	var headerLen = headerBuffer.byteLength;
	//merge header and body
	var buffer = new ArrayBuffer(headerBuffer.byteLength + body.byteLength);
	var view = new Uint8Array(buffer); 
	for(var i=0;i<headerBuffer.byteLength;i++){
		view[i] = headerBuffer[i];
	}
	
	for(var i=0;i<body.byteLength;i++){
		view[headerLen+i] = body[i];
	}
	return buffer; 
};

 
function httpParse(data){
	if(typeof data == "string"){
		data = new TextEncoder("utf8").encode();
	} else if (data instanceof Uint8Array || data instanceof Int8Array){
		//ignore
	} else if (data instanceof ArrayBuffer) {
		data = new Uint8Array(data);
	} else {
		//not support type
		return null;
	}
	var i = 0, pos = -1; 
	var CR = 13, NL = 10;
	while(i+3<data.byteLength){
		if(data[i]==CR && data[i+1]==NL && data[i+2]==CR && data[i+3]==NL){
			pos = i; 
			break;
		}
		i++;
	} 
	if(pos == -1) return null;
	
	var str = new TextDecoder("utf8").decode(data.slice(0, pos));
	
	var blocks = str.split("\r\n");
	var lines = [];
	for(var i in blocks){
		var line = blocks[i];
		if(line == '') continue;
		lines.push(line);
	}
	
	var msg = {};
	//parse first line 
	var bb = lines[0].split(" "); 
	if(bb[0].toUpperCase().startsWith("HTTP")){
		msg.status = bb[1];  
	} else {
		msg.method = bb[0];
		msg.url = bb[1];
	}
	var typeKey = "content-type";
	var typeVal = "text/html";
	var lenKey = "content-length";
	var lenVal = 0;
	
	for(var i=1;i<lines.length;i++){
		var line = lines[i]; 
		var p = line.indexOf(":");
		if(p == -1) continue;
		var key = line.substring(0, p).trim().toLowerCase();
		var val = line.substring(p+1).trim(); 
		if(key == lenKey){
			lenVal = parseInt(val);
			continue;
		} 
		if(key == typeKey){
			typeVal = val;
			continue;
		}
		
		key = underscore2camel(key);
		msg[key] = val;
	} 
	if(pos+4+lenVal > data.byteLength){
		return null;
	}
	var encoding = msg.encoding;
	if(!encoding) encoding = "utf8";
	var decoder = new TextDecoder(encoding);
	var bodyData = data.slice(pos+4);
	if(typeVal == "text/html"){
		msg.body = decoder.decode(bodyData);
	} else if(typeVal == "application/json"){
		msg.body = JSON.parse(decoder.decode(bodyData));
	} else {
		msg.body = bodyData.buffer;
	} 
	
	return msg;	 
}




