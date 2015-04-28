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
    if(this.uri){
    	return util.format("%s %s HTTP/1.1", this.method, this.uri); 
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

Message.HEARTBEAT         = "heartbeat"; //心跳消息
//标准HTTP头部内容
Message.HEADER_CLIENT     = "remote-addr";
Message.HEADER_ENCODING   = "content-encoding";
//常见扩展HTTP协议头部
Message.HEADER_CMD 		  = "cmd";
Message.HEADER_SUBCMD     = "sub_cmd";
Message.HEADER_BROKER     = "broker";
Message.HEADER_TOPIC      = "topic"; //使用,分隔 
Message.HEADER_MQ_REPLY   = "mq_reply";
Message.HEADER_MQ         = "mq";
Message.HEADER_TOKEN      = "token";
Message.HEADER_MSGID      = "msgid";	
Message.HEADER_MSGID_RAW  = "msgid_raw";
Message.HEADER_ACK        = "ack";	
Message.HEADER_REPLY_CODE = "reply_code";

	
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
    return this.getHeadOrParam(Message.HEADER_CMD);
};
Message.prototype.setCommand = function(val){
	this.setHead(Message.HEADER_CMD, val);
};
Message.prototype.getSubCommand = function(){
    return this.getHeadOrParam(Message.HEADER_SUBCMD);
};
Message.prototype.setSubCommand = function(val){
	this.setHead(Message.HEADER_SUBCMD, val);
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
    reqMsg.setMsgId(this.id);
}

function RemotingClient(address){
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
util.inherits(RemotingClient, Events.EventEmitter);

RemotingClient.prototype.connect = function(connectedCallback){
    console.log("Trying to connect: "+this.serverHost+":"+this.serverPort);
    this.socket = Socket.connect({host: this.serverHost, port: this.serverPort});
    var clientReadBuf = this.readBuf;
    var clientTicketTable = this.ticketTable;

    var client = this;
    this.socket.on("connect", function(){
        console.log("RemotingClient connected: "+client.serverHost+":"+client.serverPort);
        connectedCallback();
        client.heartbeatInterval = setInterval(function(){
            var msg = new Message();
            msg.setCommand(Message.HEARTBEAT);
            client.invoke(msg);
        },10*1000);
    });

    this.socket.on("error", function(error){
        clearInterval(client.heartbeatInterval);
        client.socket.destroy();
        client.socket = null;
        if(client.autoReconnect){
            setTimeout(function(){
                client.connect(connectedCallback);
            }, client.reconnectInterval);
        }
        client.emit("error", error);
    });

    client.on("error", function(error){
        //ignore
    });

    this.socket.on("data", function(data){
        clientReadBuf.put(data);
        var tempBuf = clientReadBuf.duplicate();
        tempBuf.flip();

        while(true){
            var msg = Message.decode(tempBuf);
            if(msg == null) break;
            var msgid = msg.getMsgId();
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

RemotingClient.prototype.invoke = function(msg, callback){
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
Proto.Request     = "request";     //请求等待应答消息
Proto.Heartbeat   = "heartbeat";   //心跳消息
Proto.Admin       = "admin";       //管理类消息
Proto.CreateMQ    = "create_mq";
//TrackServer通讯
Proto.TrackReport = "track_report";
Proto.TrackSub    = "track_sub";
Proto.TrackPub    = "track_pub";

Proto.buildSubCommandMessage = function(cmd, subCmd, params){
    var msg = new Message();
    msg.setCommand(cmd); 
	msg.setSubCommand(subCmd); 
	var json = JSON.stringify(params);
	msg.setJsonBody(json);
    return msg;
};


function MessageMode(){}
MessageMode.MQ     = 1<<0;
MessageMode.PubSub = 1<<1;
MessageMode.Temp   = 1<<2;

function Producer(client, mq){
    this.client = client;
    this.mq = mq;
    this.token = "";
    this.mode = 0;
    var args = Array.prototype.slice.call(arguments, 2);
    for(var i in args){
        this.mode |= args[i];
    }
}
Producer.prototype.setToken = function(token){
    this.token = token;
};

Producer.prototype.send = function(msg, callback){
    msg.setCommand(Proto.Produce);
    msg.setMq(this.mq);
    msg.setToken(this.token);
    this.client.invoke(msg, callback);
};


function Consumer(client, mq){
    this.client = client;
    this.mq = mq;
    this.accessToken = "";
    this.registerToken = "";
    this.mode = 0;
    this.autoRegister = true;

    var args = Array.prototype.slice.call(arguments, 2);
    for(var i in args){
        this.mode |= args[i];
    }
}
Consumer.prototype.recv = function(callback){
    var msg = new Message();
    msg.setCommand(Proto.Consume);
    msg.setMq(this.mq);
    msg.setToken(this.accessToken);

    var consumer = this;
    this.client.invoke(msg, function(res){
        if(res.isStatus404() && consumer.autoRegister){
            consumer.register(function(registerRes){
                console.log(registerRes.toString());
                consumer.recv(callback);
            });
            return;
        }

        try{
            callback(res);
        } catch (error){
            console.log(error);
        }
        return consumer.recv(callback);
    });
};

Consumer.prototype.reply = function(msg){
    var status = msg.getStatus();
    if(!status){
        status = "200"; //assume to be "OK"
    }
    msg.setHead(Message.HEADER_REPLY_CODE, status);
    msg.setCommand(Proto.Produce);
    msg.setAck(false);
    this.client.invoke(msg);
};
Consumer.prototype.setAccessToken = function(token){
    this.accessToken = token;
};
Consumer.prototype.setRegisterToken = function(token){
    this.registerToken = token;
};

Consumer.prototype.register = function(callback){
    var params = {};
    params["mqName"] = this.mq;
    params["accessToken"] = this.accessToken;
    params["mqMode"] = ""+this.mode;

    var msg = Proto.buildSubCommandMessage(Proto.Admin, Proto.CreateMQ, params);
	msg.setToken(this.registerToken);
	
    this.client.invoke(msg, callback);
};

function Caller(client, mq){
    this.client = client;
    this.mq = mq;
    this.token = "";
}
Caller.prototype.invoke = function(msg, callback){
    msg.setCommand(Proto.Request);
    msg.setMq(this.mq);
    msg.setToken(this.token);
    this.client.invoke(msg, callback);
};
Caller.prototype.setToken = function(token){
    this.token = token;
};

function Rpc(client, mq){
	Caller.call(this, client, mq);
    this.module = "";
    this.encoding = "utf-8";
}
util.inherits(Rpc, Caller);
Rpc.prototype.setModule = function(module){
    this.module = module;
};
Rpc.prototype.setEncoding = function(encoding){
    this.encoding = encoding;
};

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
    this.accessToken = "";
    this.registerToken = "";
}
Service.prototype.setAccessToken = function(token){
    this.accessToken = token;
};
Service.prototype.setRegisterToken = function(token){
    this.registerToken = token;
};

Service.prototype.serve = function(handler){
    var consumer = new Consumer(this.client, this.mq);
    consumer.setAccessToken(this.accessToken);
    consumer.setRegisterToken(this.registerToken);

    consumer.recv(function(msg){
        console.log(msg);
        var mqReply = msg.getMqReply();
        var msgId = msg.getMsgIdRaw();
        var res = handler(msg);
        if(res){
            res.setMq(mqReply);
            res.setMsgId(msgId);
            consumer.reply(res);
        }
    });
};



exports.Message = Message;
exports.RemotingClient = RemotingClient;
exports.MessageMode = MessageMode;
exports.Producer = Producer;
exports.Consumer = Consumer;
exports.Caller = Caller;
exports.Rpc = Rpc;
exports.Service = Service;