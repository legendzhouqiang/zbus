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



// /////////////////////////////////////////////////////////////////

function Meta(meta){
	this.status = null;
	
	this.method = "GET"; 
	this.url = "/";
	this.path = null;
	this.params = null; 
	
	if(!meta || meta=="") return;
	
	var blocks = meta.split(" ");
	var method = blocks[0]; 
	if(Meta.HttpMethod.indexOf(method) == -1){
		this.status = blocks[1];
		return;
	}
	this.url = blocks[1];
	this.decodeUrl(this.url);
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
    	return "HTTP/1.1 {0} {1}".format(this.status, desc); 
    }
    return "{0} {1} HTTP/1.1".format(this.method, this.url); 
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

Meta.prototype.setUrl = function(url){
	this.url = url;
	this.decodeUrl(url);
}

Meta.prototype.decodeUrl = function(cmdStr){
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


//HTTP Message

function Message(body){
	this.meta = new Meta();
	this.head = {};
	this.setBody(body);
}

Message.HEARTBEAT   = "heartbeat"; 
Message.REMOTE_ADDR = "remote-addr";
Message.ENCODING    = "encoding"; 

Message.CMD     	  = "cmd"; 
Message.BROKER  	  = "broker";
Message.TOPIC   	  = "topic"; 
Message.MQ      	  = "mq"; 
Message.ID      	  = "id";	 
Message.ACK     	  = "ack";	 
Message.SENDER  	  = "sender";
Message.RECVER  	  = "recver";
Message.ORIGIN_URL    = "origin_url";
Message.ORIGIN_ID     = "rawid";
Message.ORIGIN_STATUS = "reply_code"

	
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

Message.prototype.getOriginUrl = function(){
    return this.getHead(Message.ORIGIN_URL);
};
Message.prototype.setOriginUrl = function(val){
	this.setHead(Message.ORIGIN_URL, val);
};

Message.prototype.getOriginStatus = function(){
    return this.getHead(Message.ORIGIN_STATUS);
};
Message.prototype.setOriginStatus = function(val){
	this.setHead(Message.ORIGIN_STATUS, val);
}; 

Message.prototype.getOriginId = function(){
    return this.getHead(Message.ORIGIN_ID);
};
Message.prototype.setOriginId = function(val){
	this.setHead(Message.ORIGIN_ID, val);
};


Message.prototype.getPath = function(){
    return this.meta.path;
};
Message.prototype.getUrl = function(){
    return this.meta.url;
};
Message.prototype.setUrl = function(url){
	this.meta.status = null;
    return this.meta.setUrl(url);
};
Message.prototype.getStatus = function(){
    return this.meta.status;
};
Message.prototype.setStatus = function(val){ 
	this.meta.status = val;
};
Message.prototype.getBodyString = function(){
    if(!this.body) return null;
    return buffer2String(this.body);
};

Message.prototype.getBody = function(){
    if(!this.body) return null;
    return this.body;
};

Message.prototype.setBody = function(val){ 
	if(val === undefined) return;
	if(val instanceof Int16Array){
		this.body = val; 
	} else {
		this.body = string2Buffer(val);
	} 
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


Message.prototype.toString = function(){
	var lines = new Array();
	lines.push("{0}".format(this.meta.toString()));

	for(var key in this.head){
		lines.push("{0}: {1}".format(key, this.head[key]));
	} 
	var bodyLen = 0;
	if(this.body){
		bodyLen = this.body.length;
	} 
	var lenKey = "content-length";
	if(!(lenKey in this.head)){
		lines.push("{0}: {1}".format(lenKey, bodyLen)); 
	}  
	var bodyString = "\r\n";
	if(bodyLen > 0){
		bodyString += buffer2String(this.body);
	} 
	lines.push(bodyString);

	return lines.join("\r\n");
};

Message.parse = function(str){
	var blocks = str.split("\r\n");
	var lines = [];
	for(var i in blocks){
		var line = blocks[i];
		if(line == '') continue;
		lines.push(line);
	}
	var lenKey = "content-length";
	var lenVal = 0;
	var msg = new Message();
	msg.meta = new Meta(lines[0]);
	for(var i=1;i<lines.length;i++){
		var line = lines[i];
		if(i == lines.length-1){
			if(lenVal > 0){
				msg.setBody(line);
				continue;
			}
		}
		
		var p = line.indexOf(":");
		if(p == -1) continue;
		var key = line.substring(0, p).trim().toLowerCase();
		var val = line.substring(p+1).trim();
		if(key == lenKey){
			lenVal = val;
		}

		msg.setHead(key, val);
	}

	return msg;	
};

function Ticket(reqMsg, callback){
    this.id = uuid();
    this.request = reqMsg;
    this.response = null;
    this.callback = callback;
    reqMsg.setId(this.id);
}

var WebSocket = window.WebSocket;
if(!WebSocket){
	WebSocket = window.MozWebSocket;
}
function MessageClient(address){   
	this.address = address;
    this.autoReconnect = true;
    this.reconnectInterval = 3000; 
    this.ticketTable = {}; 
    
}

MessageClient.prototype.connect = function(connectedHandler){
	console.log("Trying to connect to "+this.address);
	
	this.socket = new WebSocket(this.address); 
	var client = this;
    this.socket.onopen = function(event){
    	console.log("Connected to " + client.address);
    	if(connectedHandler){
    		connectedHandler(event);
    	}
    	client.heartbeatInterval = setInterval(function(){
            var msg = new Message();
            msg.setCmd(Message.HEARTBEAT);
            client.invokeAsync(msg);
        },300*1000);
    };
    
    this.socket.onclose = function(event){
    	clearInterval(client.heartbeatInterval);
    	setTimeout(function(){
    		try{ client.connect(connectedHandler); } catch(e){}//ignore
    	}, client.reconnectInterval);
    }; 
    
    this.socket.onmessage = function(event){
    	var msg = Message.parse(event.data);
    	var msgid = msg.getId();
        var ticket = client.ticketTable[msgid];
        if(ticket){
            ticket.response = msg;
            if(ticket.callback){
                ticket.callback(msg);
            }
            delete client.ticketTable[msgid];
        } else {
        	console.log("Warn: drop message\n"+ msg.toString());
        }
    }
    
    this.socket.onerror = function(data){
    	console.log("Error: "+data);
    }
}

MessageClient.prototype.invokeAsync = function(msg, callback){
	if (this.socket.readyState != WebSocket.OPEN) {
		console.log("socket is not open, invalid");
		return;
	}
    if(callback){
        var ticket = new Ticket(msg, callback);
        this.ticketTable[ticket.id] = ticket;
    } 
    this.socket.send(msg);  
};


function Proto(){}
Proto.Produce     = "produce";     
Proto.Consume     = "consume";  
Proto.Route       = "route";  
Proto.Heartbeat   = "heartbeat";   
Proto.Admin       = "admin";     
Proto.CreateMQ    = "create_mq";

function MqMode(){}
MqMode.MQ     = 1<<0;
MqMode.PubSub = 1<<1;
MqMode.Memory = 1<<2;

var Broker = MessageClient;
//define more brokers

function MqAdmin(broker, mq){
	this.broker = broker;
	this.mq = mq;
	this.mode = 0;
    var args = Array.prototype.slice.call(arguments, 2);
    for(var i in args){
        this.mode |= args[i];
    } 
} 

MqAdmin.prototype.createMq = function(callback){
    var params = {};
    params["mq_name"] = this.mq; 
    params["mq_mode"] = ""+this.mode;
    
    var msg = new Message();
    msg.setCmd(Proto.CreateMQ);
    msg.setHead("mq_name", this.mq);
    msg.setHead("mq_mode", ""+this.mode);
    
    this.broker.invokeAsync(msg, callback);
};


function Producer(broker, mq){
	MqAdmin.call(this, broker, mq); 
}
inherits(Producer, MqAdmin)

Producer.prototype.sendAsync = function(msg, callback){
    msg.setCmd(Proto.Produce);
    msg.setMq(this.mq); 
    this.broker.invokeAsync(msg, callback);
};

function Consumer(broker, mq){
	MqAdmin.call(this, broker, mq); 
}

inherits(Consumer, MqAdmin);

Consumer.prototype.take = function(callback){
    var msg = new Message();
    msg.setCmd(Proto.Consume);
    msg.setMq(this.mq); 
    if(this.topic) msg.setTopic(this.topic);

    var consumer = this;
    this.broker.invokeAsync(msg, function(res){
        if(res.isStatus404()){
            consumer.createMq(function(res){ 
            	if(res.isStatus200()){
            		console.log(consumer.mq + " created");
            	}
                consumer.take(callback);
            });
            return;
        } 
        if(res.isStatus200()){
        	var originUrl = res.getOriginUrl();
        	var id = res.getOriginId();
        	res.removeHead(Message.ORIGIN_ID);
        	if(originUrl == null){
        		originUrl = "/";
        	} else {
        		res.removeHead(Message.ORIGIN_URL);
        	}
        	
        	res.setId(id);  
        	res.setUrl(originUrl);
        	try{  
                callback(res);
            } catch (error){
                console.log(error);
            }
        }
        
        return consumer.take(callback);
    });
};

Consumer.prototype.route = function(msg){
    msg.setCmd(Proto.Route);
    msg.setAck(false);
    this.broker.invokeAsync(msg);
}; 

 

function Rpc(client, mq){
	MqAdmin.call(this, client, mq);  
    this.module = "";
    this.encoding = "utf-8";
}
inherits(Rpc, MqAdmin); 

function uint8Array2String(buf, encoding) {
	var decoder = new TextDecoder(encoding);
	return decoder.decode(buf);
}

Rpc.prototype.invoke = function(jsonReq, callback){
    if(!jsonReq.module){
        jsonReq.module = this.module;
    }
    var msg = new Message();
    msg.setBody(JSON.stringify(jsonReq));
    
    msg.setCmd(Proto.Produce);
    msg.setMq(this.mq); 
    msg.setAck(false);
    this.broker.invokeAsync(msg, function(msg){ 
    	var jsonString = uint8Array2String(msg.getBody(), 'utf8');
    	console.log(jsonString);
    	var object = JSON.parse(jsonString);
    	callback(object);
    });
}; 






