var util = require("util");
var Buffer = require("buffer").Buffer;

////////////////////////////UTILS///////////////////////////////
function stringEndsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
}
function hashSize(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
}
//=================IoBuffer,类似Java NIO ByteBuffer===============
function IoBuffer(capacity){
	if(capacity == undefined){
		capacity = 512;
	}
	this.capacity = capacity;
	this.data = new Buffer(capacity);
	this.position = 0;
	this.limit = capacity;
	this.mark = -1;
} 
IoBuffer.prototype.mark = function(){
	this.mark = this.position;
};

IoBuffer.prototype.reset = function(){
	var m = this.mark;
	if(m<0){
		throw new Error("mark not set, should not reset");
	}
	this.position = m;
};

IoBuffer.prototype.remaining = function(){
	return this.limit - this.position;
};

IoBuffer.prototype.flip = function(){
	this.limit = this.position;
	this.position = 0;
	this.mark = -1;
};

IoBuffer.prototype.newLimit = function(newLimit){
	if(newLimit>this.capacity || newLimit<0){
		throw new Error("set new limit error");
	}
	this.limit = newLimit;
	if(this.position > this.limit) this.position = this.limit;
	if(this.mark > this.limit) this.mark = -1;
};

IoBuffer.prototype.autoExpand = function(need){
	var newCap = this.capacity;
	var newSize = this.position + need;
	while(newSize > newCap){
		newCap *= 2;
	}

	if(newCap == this.capacity) return;
	var newData = new Buffer(newCap); 
	this.data.copy(newData, 0, 0, this.position);
	this.data = newData;
	this.capacity = newCap;
	this.limit = newCap;
};
IoBuffer.prototype.drain = function(n){
	if(n<=0) return;
	var newPos = this.position + n;
	if(newPos > this.limit){
		newPos = this.limit;
	}
	this.position = newPos;
};
IoBuffer.prototype.put = function(val){
	var len = val.length;
	this.autoExpand(len);
	if(Buffer.isBuffer(val)){
		val.copy(this.data, this.position, 0, len);
	} else {
		this.data.write(val, this.position, len);
	} 
	this.position += len;
};



//===================HTTP头部第一行解释,Meta=======================
function Meta(meta){
	this.status = null;
	
	this.method = "GET";
	this.command = null;
	this.params = null; 
	
	if(!meta || meta=="") return;
	
	var blocks = meta.split(" ");
	var method = blocks[0]; 
	if(Meta.HttpMethod.indexOf(method) == -1){
		this.status = blocks[1];
		return;
	}
	this.decodeCommand(blocks[1]);
}
Meta.HttpMethod = ["GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS"];
Meta.HttpStatus = {
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
Meta.prototype.toString = function(){
    if(this.status){
    	var desc = Meta.HttpStatus[this.status];
    	if(!desc) desc = "Unknown Status";
    	return util.format("HTTP/1.1 %s %s", this.status, desc); 
    }
    if(this.command){
    	var cmdString = this.encodeCommand();
    	return util.format("%s /%s HTTP/1.1", this.method, cmdString); 
    }
    return "";
};
Meta.prototype.getParam = function(key){
	if(!this.params){
        return undefined;
    }
	return this.params[key];
};
Meta.prototype.setParam = function(key, val){
	if(!this.params){
		this.params = {};
	}
	this.params[key] = val;
};
Meta.prototype.encodeCommand = function(){
	var res = "";
	if(this.command){
		res += this.command;
	}
	if(this.params){
		if(hashSize(this.params)>0){
			res += "?";
		}
		for(var key in this.params){
			res += util.format("%s=%s&", key, this.params[key]);
		}
		if(stringEndsWith(res, "&")){
			res = res.substring(0, res.length-1);
		}
	}
	return res;
};
Meta.prototype.decodeCommand = function(cmdStr){
	var idx = cmdStr.indexOf("?");
	if(idx<0){
		this.commond = cmdStr;
	} else {
		this.command = cmdStr.substring(0, idx);
	}
	if(this.command.charAt(0) == '/'){
		this.command = this.command.substring(1);
	}
	if(idx<0) return;
	
	var paramStr = cmdStr.substring(idx+1);
	this.params = {};
	var kvs = paramStr.split("&"); 
	for(var i in kvs){ 
		var kv = kvs[i];
		idx = kv.indexOf("=");
		if(idx<0){
			util.debug("omit: "+kv);
			continue;
		}
		var key = kv.substring(0,  idx);
		var val = kv.substring(idx+1);
		this.params[key] = val;
	} 
};

//===================HTTP消息格式,头部行集+消息体=======================
function Message(){
	this.meta = new Meta();
	this.head = {};
	this.body = null; //Buffer类型(Node.JS)
}

Message.HEARTBEAT         = "heartbeat"; //心跳消息
//标准HTTP头部内容
Message.HEADER_CLIENT     = "remote-addr";
Message.HEADER_ENCODING   = "content-encoding";
//常见扩展HTTP协议头部
Message.HEADER_BROKER     = "broker";
Message.HEADER_TOPIC      = "topic"; //使用,分隔 
Message.HEADER_MQ_REPLY   = "mq-reply";
Message.HEADER_MQ         = "mq";
Message.HEADER_TOKEN      = "token";
Message.HEADER_MSGID      = "msgid";	
Message.HEADER_MSGID_RAW  = "msgid-raw";
Message.HEADER_ACK        = "ack";	
Message.HEADER_REPLY_CODE = "reply-code";
	
Message.prototype.getHead = function(key){
    return this.head[key];
};

Message.prototype.getHeadOrParam = function(key){
    var val = this.head[key];
    if(!val) val = this.meta.getParam(key);
    return val;
}
Message.prototype.setHead = function(key, val){
    this.head[key] = val;
};

Message.prototype.getMq = function(){
    return this.getHeadOrParam(Message.HEADER_MQ);
};
Message.prototype.setMq = function(val){
	this.setHead(Message.HEADER_MQ, val);
};
Message.prototype.getMqReply = function(){
    return this.getHeadOrParam(Message.HEADER_MQ_REPLY);
};
Message.prototype.setMqReply = function(val){
	this.setHead(Message.HEADER_MQ_REPLY, val);
};
Message.prototype.getMsgId = function(){
    return this.getHeadOrParam(Message.HEADER_MSGID);
};
Message.prototype.setMsgId = function(val){
	this.setHead(Message.HEADER_MSGID, val);
};
Message.prototype.getMsgIdRaw = function(){
    return this.getHeadOrParam(Message.HEADER_MSGID_RAW);
};
Message.prototype.setMsgIdRaw = function(val){
	this.setHead(Message.HEADER_MSGID_RAW, val);
};
Message.prototype.getToken = function(){
    return this.getHeadOrParam(Message.HEADER_TOKEN);
};
Message.prototype.setToken = function(val){
	this.setHead(Message.HEADER_TOKEN, val);
};
Message.prototype.getTopic = function(){
    return this.getHeadOrParam(Message.HEADER_TOPIC);
};
Message.prototype.setTopic = function(val){
	this.setHead(Message.HEADER_TOPIC, val);
};
Message.prototype.getEncoding = function(){
    return this.getHeadOrParam(Message.HEADER_ENCODING);
};
Message.prototype.setEncoding = function(val){
	this.setHead(Message.HEADER_ENCODING, val);
};
Message.prototype.isAck = function(){
    var ack = this.getHeadOrParam(Message.HEADER_ACK);
    if(!ack) return true;//default to true
    return ack == '1';
};
Message.prototype.setAck = function(val){
	this.setHead(Message.HEADER_ACK, val);
};
Message.prototype.getCommand = function(){
    return this.meta.command;
};
Message.prototype.setCommand = function(val){
	this.meta.status = null;
	this.meta.command = val;
};
Message.prototype.getStatus = function(){
    return this.meta.status;
};
Message.prototype.setStatus = function(val){
	this.meta.command = null;
	this.meta.status = val;
};
Message.prototype.setBody = function(val){
	if(!Buffer.isBuffer(val)){
		val = new Buffer(val);
	}
	this.body = val;
	this.setHead('content-length', this.body.length);
};
Message.prototype.setBodyFormat = function(format){
    var args = Array.prototype.slice.call(arguments, 1);
    var body = format.replace(/{(\d+)}/g, function(match, number) {
        return typeof args[number] != 'undefined'
            ? args[number]
            : match
            ;
    });
    this.setBody(body);
};
Message.prototype.encode = function(){
	var iobuf = new IoBuffer();
	iobuf.put(util.format("%s\r\n", this.meta.toString()));
	for(var key in this.head){
		iobuf.put(util.format("%s: %s\r\n", key, this.head[key]));
	} 
	var bodyLen = 0;
	if(this.body){
		bodyLen = this.body.length;
	} 
	var lenKey = "content-length"; 
	if(!(lenKey in this.head)){
		iobuf.put(util.format("%s: %s\r\n", lenKey, bodyLen));
	}
	if(bodyLen > 0){
		iobuf.put("\r\n");
		iobuf.put(this.body);
	} 
	iobuf.flip();
	return iobuf;
};
Message.findHeaderEnd = function(iobuf){
	var i = iobuf.position;
	var data = iobuf.data;
	var CR = 13, NL = 10;
	while(i+3<iobuf.limit){
		if(data[i]==CR && data[i+1]==NL && data[i+2]==CR && data[i+3]==NL){
			return i+3;
		}
		i++;
	}
	return -1;
}

Message.decodeHeaders = function( headerStr ){
	var blocks = headerStr.split("\r\n");
	var lines = [];
	for(var i in blocks){
		var line = blocks[i];
		if(line == '') continue;
		lines.push(line);
	}
	
	var msg = new Message();
	msg.meta = new Meta(lines[0]);
	for(var i=1;i<lines.length;i++){
		var line = lines[i];
		var p = line.indexOf(":");
		if(p == -1) continue;
		var key = line.substring(0, p).trim().toLowerCase();
		var val = line.substring(p+1).trim();
		msg.setHead(key, val);
	}
	return msg;
};

Message.decode = function(iobuf){
	var headerIdx = Message.findHeaderEnd(iobuf); 
	if(headerIdx == -1) return null;
	var headLen = headerIdx+1 - iobuf.position;
	
	var headerStr = iobuf.data.slice(iobuf.position,  headerIdx+1).toString();
	var msg = Message.decodeHeaders(headerStr); 
	var lenKey = "content-length";
	var bodyLen = msg.getHead(lenKey);
    bodyLen = parseInt(bodyLen);
	if(bodyLen == undefined){
		iobuf.drain(headLen);
		return msg;
	}
	if(iobuf.remaining() < (headLen+bodyLen)){
		return null;
	}
	iobuf.drain(headLen);
	msg.body = new Buffer(bodyLen);
	msg.body = iobuf.data.slice(iobuf.position, iobuf.position+bodyLen);
	iobuf.drain(bodyLen);
	return msg;
};


function RemotingClient(address){
    var p = address.indexOf(':');
    if(p == -1){
        this.serverHost = address.trim();
        this.serverPort = 15555;
    } else {
        this.serverHost = address.substring(0, p).trim();
        this.serverPort = parseInt(address.substring(p+1).trim());
    }
    this.readTimeout = 3000;
    this.connectTimeout = 3000;

    this.msgCallback = undefined;
    this.connectCallback = undefined;
    this.errorCallback = undefined;

}

RemotingClient.prototype.connect = function(){

};
var msg = new Message();
msg.setCommand("produce");
msg.setMq("MyMQ");
msg.setTopic("qhee");
msg.setBodyFormat("hello world from node.js, {0}", new Date().getTime());

var client = new RemotingClient("127.0.0.1:15555");
console.log(client);