////////////////////////////UTILS///////////////////////////////
function formatString(format) {
    var args = Array.prototype.slice.call(arguments, 1);
    return format.replace(/{(\d+)}/g, function(match, number) { 
      return typeof args[number] != 'undefined'
        ? args[number] 
        : match
      ;
    });
}
function stringEndsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
}

function hashSize(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
};
/////////////////////////////////////////////////////////////////// 
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
}

Meta.prototype.toString = function(){
    if(this.status){
    	var desc = Meta.HttpStatus[this.status];
    	if(!desc) desc = "Unknown Status";
    	return formatString("HTTP/1.1 {0} {1}", this.status, desc); 
    }
    if(this.command){
    	var cmdString = this.encodeCommand();
    	return formatString("{0} /{1} HTTP/1.1", this.method, cmdString); 
    }
    return "";
}
Meta.prototype.getParam = function(key){
	return this.getParamWithDefault(key, null);
}
Meta.prototype.getParamWithDefault = function(key, defaultValue){ 
	if(!this.params){
		return defaultValue;
	}
	var val = this.params[key];
	if(!val) val = defaultValue;
	return val;
}
Meta.prototype.setParam = function(key, val){
	if(!this.params){
		this.params = {};
	}
	this.params[key] = val;
}


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
			res += formatString("{0}={1}&", key, this.params[key]);
		}
		if(stringEndsWith(res, "&")){
			res = res.substring(0, res.length-1);
		}
	}
	return res;
}
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
			console.log("omit: "+kv);
			return;
		}
		var key = kv.substring(0,  idx);
		var val = kv.substring(idx+1);
		this.params[key] = val;
	} 
}


function Message(){
	this.meta = new Meta();
	this.head = {};
	this.body = new ArrayBuffer(0);
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
}

Message.prototype.getHeadOrParam = function(key){
    var val = this.head[key];
    if(!val) val = this.meta.getParam(key);
    return val;
}
Message.prototype.setHead = function(key, val){
    this.head[key] = val;
}

Message.prototype.getMq = function(){
    return this.getHeadOrParam(Message.HEADER_MQ);
} 
Message.prototype.setMq = function(val){
	this.setHead(Message.HEADER_MQ, val);
}
Message.prototype.getMqReply = function(){
    return this.getHeadOrParam(Message.HEADER_MQ_REPLY);
} 
Message.prototype.setMqReply = function(val){
	this.setHead(Message.HEADER_MQ_REPLY, val);
}
Message.prototype.getMsgId = function(){
    return this.getHeadOrParam(Message.HEADER_MSGID);
} 
Message.prototype.setMsgId = function(val){
	this.setHead(Message.HEADER_MSGID, val);
}
Message.prototype.getMsgIdRaw = function(){
    return this.getHeadOrParam(Message.HEADER_MSGID_RAW);
} 
Message.prototype.setMsgIdRaw = function(val){
	this.setHead(Message.HEADER_MSGID_RAW, val);
}
Message.prototype.getToken = function(){
    return this.getHeadOrParam(Message.HEADER_TOKEN);
} 
Message.prototype.setToken = function(val){
	this.setHead(Message.HEADER_TOKEN, val);
}
Message.prototype.getTopic = function(){
    return this.getHeadOrParam(Message.HEADER_TOPIC);
} 
Message.prototype.setTopic = function(val){
	this.setHead(Message.HEADER_TOPIC, val);
}
Message.prototype.getEncoding = function(){
    return this.getHeadOrParam(Message.HEADER_ENCODING);
} 
Message.prototype.setEncoding = function(val){
	this.setHead(Message.HEADER_ENCODING, val);
}
Message.prototype.isAck = function(){
    var ack = this.getHeadOrParam(Message.HEADER_ACK);
    if(!ack) return true;//default to true
    return ack == '1';
} 
Message.prototype.setAck = function(val){
	this.setHead(Message.HEADER_ACK, val);
}

Message.prototype.getCommand = function(){
    return this.meta.command;
} 
Message.prototype.setCommand = function(val){
	this.meta.status = null;
	this.meta.command = val;
}
Message.prototype.getStatus = function(){
    return this.meta.status;
} 
Message.prototype.setStatus = function(val){
	this.meta.command = null;
	this.meta.status = val;
}

 
var msg = new Message(); 
msg.setCommand("produce"); 
console.log(msg);