var util = require("util");
var Events = require('events');
var Buffer = require("buffer").Buffer;
var Socket = require("net");

//============================基本工具函数=============================
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

//=================IoBuffer,类似Java NIO ByteBuffer===============
function IoBuffer(capacity, buf){
	if(capacity == undefined){
		capacity = 512;
	}
	this.capacity = capacity;
    if(buf === undefined){
        this.data = new Buffer(capacity);
    } else {
        if(!Buffer.isBuffer(buf)){
            throw new TypeError("buf should be Node.JS Buffer type")
        }
        if(buf.length != this.capacity){
            throw new RangeError("capacity should be same as buf")
        }
        this.data = buf;
    }

	this.position = 0;
	this.limit = capacity;
	this.mark = -1;
}

IoBuffer.prototype.duplicate = function(){
    var dup = new IoBuffer(this.capacity, this.data);
    dup.position = this.position;
    dup.limit = this.limit;
    dup.mark = this.mark;
    return dup;
};
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

IoBuffer.prototype.move = function(n){
    if(n > this.position){
        throw new RangeError("move back range exceed position");
    }
    this.data.copy(this.data, 0, n, this.position);
    this.position -= n;
};


IoBuffer.prototype.remainingBuf = function(){
    return this.data.slice(this.position, this.limit);
}



//===================HTTP头部第一行解释,Meta=======================
function Meta(meta){
	this.status = null;
	
	this.method = "GET"; 
	this.uri = "/";
	this.path = null;
	this.params = null; 
	
	if(!meta || meta=="") return;
	
	var blocks = meta.split(" ");
	var method = blocks[0]; 
	if(Meta.HttpMethod.indexOf(method) == -1){
		this.status = blocks[1];
		return;
	}
	this.uri = blocks[1];
	this.decodeUri(this.uri);
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
    var uri = this.uri;
    if(uri == null){
    	uri = "/";
    }
    return util.format("%s %s HTTP/1.1", this.method, uri);  
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

Meta.prototype.decodeUri = function(cmdStr){
	var idx = cmdStr.indexOf("?");
	if(idx<0){
		this.path = cmdStr;
	} else {
		this.path = cmdStr.substring(0, idx);
	}
	if(this.path.charAt(0) == '/'){
		this.path = this.path.substring(1);
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

Message.HEARTBEAT        = "heartbeat"; //心跳消息
//标准HTTP头部内容
Message.REMOTE_ADDR      = "remote-addr";
Message.ENCODING = "encoding";
//常见扩展HTTP协议头部
Message.CMD     = "cmd";
Message.SUB_CMD = "sub_cmd";
Message.BROKER  = "broker";
Message.TOPIC   = "topic"; 
Message.MQ      = "mq"; 
Message.ID      = "id";	
Message.RAWID   = "rawid";
Message.ACK     = "ack";	 
Message.SENDER  = "sender";
Message.RECVER  = "recver";
Message.REPLY_CODE  = "reply_code";

	
Message.prototype.getHead = function(key){
    return this.head[key];
};

Message.prototype.setHead = function(key, val){
    this.head[key] = val;
};

Message.prototype.removeHead = function(key){
    delete this.head[key];
};



Message.prototype.getMq = function(){
    return this.getHead(Message.MQ);
};

Message.prototype.setMq = function(val){
	this.setHead(Message.MQ, val);
};

Message.prototype.getId = function(){
    return this.getHead(Message.ID);
};

Message.prototype.setId = function(val){
	this.setHead(Message.ID, val);
};

Message.prototype.getRawId = function(){
    return this.getHead(Message.RAWID);
};

Message.prototype.setRawId = function(val){
	this.setHead(Message.RAWID, val);
};

Message.prototype.getTopic = function(){
    return this.getHead(Message.TOPIC);
};
Message.prototype.setTopic = function(val){
	this.setHead(Message.TOPIC, val);
};
Message.prototype.getEncoding = function(){
    return this.getHead(Message.ENCODING);
};
Message.prototype.setEncoding = function(val){
	this.setHead(Message.ENCODING, val);
};
Message.prototype.getReplyCode = function(){
    return this.getHead(Message.REPLY_CODE);
};
Message.prototype.setReplyCode = function(val){
	this.setHead(Message.REPLY_CODE, val);
};
Message.prototype.isAck = function(){
    var ack = this.getHead(Message.ACK);
    if(!ack) return true;//default to true
    return ack == '1';
};
Message.prototype.setAck = function(val){
	this.setHead(Message.ACK, val);
};

Message.prototype.getCmd = function(){
    return this.getHead(Message.CMD);
};
Message.prototype.setCmd = function(val){
	this.setHead(Message.CMD, val);
};
Message.prototype.getSubCmd = function(){
    return this.getHead(Message.SUB_CMD);
};
Message.prototype.setSubCmd = function(val){
	this.setHead(Message.SUB_CMD, val);
};

Message.prototype.getSender = function(){
    return this.getHead(Message.SENDER);
};
Message.prototype.setSender = function(val){
	this.setHead(Message.SENDER, val);
};

Message.prototype.getRecver = function(){
    return this.getHead(Message.RECVER);
};
Message.prototype.setRecver = function(val){
	this.setHead(Message.RECVER, val);
};

Message.prototype.getPath = function(){
    return this.meta.path;
};
Message.prototype.getUri = function(){
    return this.meta.uri;
};
Message.prototype.getStatus = function(){
    return this.meta.status;
};
Message.prototype.setStatus = function(val){ 
	this.meta.status = val;
};
Message.prototype.getBodyString = function(){
    if(!this.body) return null;
    return this.body.toString();
};

Message.prototype.getBody = function(){
    if(!this.body) return null;
    return this.body;
};

Message.prototype.setBody = function(val){
	if(!Buffer.isBuffer(val)){
		val = new Buffer(val);
	}
	this.body = val;
	this.setHead('content-length', this.body.length);
};
Message.prototype.setJsonBody = function(json){
	this.setBody(json);
	this.setHead('content-type', 'application/json');
}
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

Message.prototype.isStatus200 = function(){
    return "200" == this.getStatus();
};
Message.prototype.isStatus404 = function(){
    return "404" == this.getStatus();
};
Message.prototype.isStatus500 = function(){
    return "500" == this.getStatus();
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
    iobuf.put("\r\n");
	if(bodyLen > 0){
		iobuf.put(this.body);
	} 
	iobuf.flip();
	return iobuf;
};
Message.prototype.toString = function(){
    var iobuf = this.encode();
    var buf = iobuf.remainingBuf();
    return buf.toString();
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

function Ticket(reqMsg, callback){
    this.id = uuid();
    this.request = reqMsg;
    this.response = null;
    this.callback = callback;
    reqMsg.setId(this.id);
}

function MessageClient(address){
    Events.EventEmitter.call(this);
    var p = address.indexOf(':');
    if(p == -1){
        this.serverHost = address.trim();
        this.serverPort = 15555;
    } else {
        this.serverHost = address.substring(0, p).trim();
        this.serverPort = parseInt(address.substring(p+1).trim());
    }
    this.autoReconnect = true;
    this.reconnectInterval = 3000;
    this.socket = null;
    this.ticketTable = {};
    this.readBuf = new IoBuffer();
}
util.inherits(MessageClient, Events.EventEmitter);

MessageClient.prototype.connect = function(connectedCallback){
    console.log("Trying to connect: "+this.serverHost+":"+this.serverPort);
    this.socket = Socket.connect({host: this.serverHost, port: this.serverPort});
    var clientReadBuf = this.readBuf;
    var clientTicketTable = this.ticketTable;

    var client = this;
    this.socket.on("connect", function(){
        console.log("MessageClient connected: "+client.serverHost+":"+client.serverPort);
        connectedCallback();
        client.heartbeatInterval = setInterval(function(){
            var msg = new Message();
            msg.setCmd(Message.HEARTBEAT);
            client.invoke(msg);
        },300*1000);
    });
 

    this.socket.on("error", function(error){  
        client.emit("error", error);
    });

    this.socket.on("close", function(){  
        clearInterval(client.heartbeatInterval);
        client.socket.destroy();
        client.socket = null;

        if(client.autoReconnect){
            console.log("Trying to recconnect: "+client.serverHost+":"+client.serverPort);
            setTimeout(function(){
                client.connect(connectedCallback);
            }, client.reconnectInterval);
        } 
    });


    client.on("error", function(error){
        console.log(error); 
    }); 

    this.socket.on("data", function(data){
        clientReadBuf.put(data);
        var tempBuf = clientReadBuf.duplicate();
        tempBuf.flip();

        while(true){
            var msg = Message.decode(tempBuf);
            if(msg == null) break;
            var msgid = msg.getId();
            var ticket = clientTicketTable[msgid];
            if(ticket){
                ticket.response = msg;
                if(ticket.callback){
                    ticket.callback(msg);
                }
                delete clientTicketTable[msgid];
            }
        }
        if(tempBuf.position > 0){
            clientReadBuf.move(tempBuf.position);
        }
    });
};

MessageClient.prototype.invoke = function(msg, callback){
    if(callback){
        var ticket = new Ticket(msg, callback);
        this.ticketTable[ticket.id] = ticket;
    }
    var iobuf = msg.encode();
    var buf = iobuf.remainingBuf();
    this.socket.write(buf);
};

///////////////////////////////ZBUS////////////////////////////////
function Proto(){}
Proto.Produce     = "produce";     //生产消息
Proto.Consume     = "consume";     //消费消息
Proto.Route       = "route";       //路由消息
Proto.Heartbeat   = "heartbeat";   //心跳消息
Proto.Admin       = "admin";       //管理类消息
Proto.CreateMQ    = "create_mq";

function MqMode(){}
MqMode.MQ     = 1<<0;
MqMode.PubSub = 1<<1;
MqMode.Memory = 1<<2;

function MqAdmin(client, mq){
	this.client = client;
	this.mq = mq;
	this.mode = 0;
    var args = Array.prototype.slice.call(arguments, 2);
    for(var i in args){
        this.mode |= args[i];
    } 
}
MqAdmin.prototype.createMQ = function(callback){
    var params = {};
    params["mq_name"] = this.mq; 
    params["mq_mode"] = ""+this.mode;
    
    var msg = new Message();
    msg.setCmd(Proto.CreateMQ);
    msg.setHead("mq_name", this.mq);
    msg.setHead("mq_mode", ""+this.mode);
    
    this.client.invoke(msg, callback);
};


function Producer(client, mq){
	MqAdmin.call(this, client, mq); 
}
util.inherits(Producer, MqAdmin);
Producer.prototype.send = function(msg, callback){
    msg.setCmd(Proto.Produce);
    msg.setMq(this.mq); 
    this.client.invoke(msg, callback);
};


function Consumer(client, mq){
	MqAdmin.call(this, client, mq); 
}
util.inherits(Consumer, MqAdmin);

Consumer.prototype.recv = function(callback){
    var msg = new Message();
    msg.setCmd(Proto.Consume);
    msg.setMq(this.mq); 
    if(this.topic) msg.setTopic(this.topic);

    var consumer = this;
    this.client.invoke(msg, function(res){
        if(res.isStatus404()){
            consumer.createMQ(function(res){ 
            	if(res.isStatus200()){
            		console.log(consumer.mq + " created");
            	}
                consumer.recv(callback);
            });
            return;
        } 
        try{
        	res.setId(res.getRawId()); 
        	res.removeHead(Message.RAWID);
            callback(res);
        } catch (error){
            console.log(error);
        }
        return consumer.recv(callback);
    });
};

Consumer.prototype.route = function(msg){
    msg.setCmd(Proto.Route);
    msg.setAck(false);
    if(msg.getStatus() != null){ 
    	msg.setReplyCode(msg.getStatus());
    	msg.setStatus(null);
    }
    this.client.invoke(msg);
};


function Caller(client, mq){
	MqAdmin.call(this, client, mq); 
}
util.inherits(Caller, MqAdmin);

Caller.prototype.invoke = function(msg, callback){
    msg.setCmd(Proto.Produce);
    msg.setMq(this.mq); 
    msg.setAck(false);
    this.client.invoke(msg, callback);
};


function Rpc(client, mq){
	Caller.call(this, client, mq);
    this.module = "";
    this.encoding = "utf-8";
}
util.inherits(Rpc, Caller); 

Rpc.prototype.invoke = function(jsonReq, callback){
    if(!jsonReq.module){
        jsonReq.module = this.module;
    }
    var msg = new Message();
    msg.setBody(JSON.stringify(jsonReq));
    Caller.prototype.invoke.call(this, msg, function(msg){
    	var jsonString = msg.getBody();
    	var object = JSON.parse(jsonString);
    	callback(object);
    });
}; 


function Service(client, mq){
    this.client = client;
    this.mq = mq; 
} 
Service.prototype.serve = function(handler){
    var consumer = new Consumer(this.client, this.mq); 
    
    consumer.recv(function(msg){ 
        var sender = msg.getSender();
        var msgId = msg.getId();
        var res = handler(msg);
        if(res){ 
            res.setMq(msg.getMq());
            res.setId(msgId);
            res.setRecver(sender);
            if(!res.getStatus()){
            	res.setStatus("200");
            } 
            consumer.route(res);
        }
    });
}; 


exports.Message = Message;
exports.MessageClient = MessageClient;
exports.MqMode = MqMode;
exports.MqAdmin = MqAdmin;
exports.Producer = Producer;
exports.Consumer = Consumer;
exports.Caller = Caller;
exports.Rpc = Rpc;
exports.Service = Service;