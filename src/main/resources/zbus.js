function Protocol(){}

Protocol.VERSION_VALUE = "0.8.0";       //start from 0.8.0 

/////////////////////////Command Values/////////////////////////
//MQ Produce/Consume
Protocol.PRODUCE       = "produce";   
Protocol.CONSUME       = "consume";   
Protocol.ROUTE         = "route";     //route back message to sender, designed for RPC

//Topic/ConsumeGroup control
Protocol.DECLAREC = "declare";  
Protocol.QUERY    = "query"; 
Protocol.REMOVE   = "remove";    
Protocol.EMPTY    = "empty";   

//High Availability (HA) 
Protocol.TRACK_PUB  = "track_pub"; 
Protocol.TRACK_SUB  = "track_sub";   

//Simple HTTP server command
Protocol.PING          = "ping"; //ping server, returning back server time
Protocol.INFO          = "info"; //server info 
Protocol.TRACE         = "trace";   
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

 
////////////////////////////////////////////////////////////

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
  
String.prototype.format = function() {
    var args = arguments;
    return this.replace(/{(\d+)}/g, function(match, number) { 
      return typeof args[number] != 'undefined' ? args[number] : match;
    });
} 

Date.prototype.format = function (fmt) { //author: meizz 
    var o = {
        "M+": this.getMonth() + 1, 
        "d+": this.getDate(), 
        "h+": this.getHours(), 
        "m+": this.getMinutes(), 
        "s+": this.getSeconds(),  
        "q+": Math.floor((this.getMonth() + 3) / 3),  
        "S": this.getMilliseconds()  
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
    if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
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
function httpEncode(msg) {
	var headers = "";
	var encoding = msg.encoding;
	if(!encoding) encoding = "utf8";
	var encoder = new TextEncoder(encoding);
	
	var body = msg.body;  
	var contentType = msg["content-type"];
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
				contentType = "text/plain";
			} 
			body = encoder.encode(body); 
		}  
	} else {
	    body = new Uint8Array(0);
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
		var val = msg[key];
		if(typeof val == 'undefined') continue;
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

function httpEncodeString(msg){
	var buf = httpEncode(msg);
	var encoding = msg.encoding;
	if(!encoding) encoding = "utf8";
	return new TextDecoder(encoding).decode(buf);
}
 
function httpDecode(data){
	if(typeof data == "string"){
		data = new TextEncoder("utf8").encode(data);
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


function Ticket(reqMsg, callback) {
    this.id = uuid();
    this.request = reqMsg;
    this.response = null;
    this.callback = callback;
    reqMsg.id = this.id;
} 

function MessageClient(serverAddress) { //websocket implementation
	this.scheme = "ws://";
    if (serverAddress.startsWith("ws://")) {
        this.scheme = "ws://";
        this.serverAddress = serverAddress.substring(this.scheme.length); 
    } else if (serverAddress.startsWith("wss://")) { 
        this.scheme = "wss://";
        this.serverAddress = serverAddress.substring(this.scheme.length);
    } else {
        this.serverAddress = serverAddress 
    } 
    
    this.autoReconnect = true;
    this.reconnectInterval = 3000;
    this.ticketTable = {}; 
    this.messageHandler = null;
    this.connectedHandler = null;
    this.disconnectedHandler = null;
}
MessageClient.prototype.address = function(){
	return this.scheme + this.serverAddress;
}
MessageClient.prototype.onMessage = function (messageHandler) {
	this.messageHandler = messageHandler;
}
MessageClient.prototype.onConnected = function (connectedHandler) {
	this.connectedHandler = connectedHandler;
}
MessageClient.prototype.onDisconnected = function (disconnectedHandler) {
	this.disconnectedHandler = disconnectedHandler;
}

MessageClient.prototype.sendMessage = function (msg) {
    if (this.socket.readyState != WebSocket.OPEN) {
        console.log("socket is not open, invalid");
        return;
    } 
    var buf = httpEncode(msg);
    this.socket.send(buf);
};


MessageClient.prototype.connect = function (connectedHandler) {
    console.log("Trying to connect to " + this.address());
    if(!connectedHandler){
    	connectedHandler = this.connectedHandler;
    }
    
    var WebSocket = window.WebSocket;
    if (!WebSocket) {
        WebSocket = window.MozWebSocket;
    }

    this.socket = new WebSocket(this.address());
    var client = this;
    this.socket.onopen = function (event) {
        console.log("Connected to " + client.address());
        if (connectedHandler) {
            connectedHandler(event);
        }  
        client.heartbeatInterval = setInterval(function () {
            var msg = {};
            msg.cmd = "heartbeat";
            client.invoke(msg);
        }, 30*1000);
    };

    this.socket.onclose = function (event) {
    	if(client.disconnectedHandler){
    		client.disconnectedHandler(event);
    		return;
    	}
        clearInterval(client.heartbeatInterval);
        setTimeout(function () {
            try { client.connect(connectedHandler); } catch (e) { }//ignore
        }, client.reconnectInterval);
    };

    this.socket.onmessage = function (event) {
        var msg = httpDecode(event.data);
        var msgid = msg.id;
        var ticket = client.ticketTable[msgid];
        if (ticket) {
            ticket.response = msg;
            if (ticket.callback) {
                ticket.callback(msg);
            }
            delete client.ticketTable[msgid];
        } else if(client.messageHandler){
        	client.messageHandler(msg);
        } else {
        	console.log("Warn: drop message\n" + msg.toString());
        }
    }

    this.socket.onerror = function (data) {
        console.log("Error: " + data);
    }
} 

MessageClient.prototype.invoke = function (msg, callback) {
    if (this.socket.readyState != WebSocket.OPEN) {
        console.log("socket is not open, invalid");
        return;
    }
    if (callback) {
        var ticket = new Ticket(msg, callback);
        this.ticketTable[ticket.id] = ticket;
    }
    var buf = httpEncode(msg);
    this.socket.send(buf);
};

MessageClient.prototype.close = function(){
	clearInterval(this.heartbeatInterval);
	this.socket.onclose = function () {}
	this.socket.close();
}

MessageClient.prototype.queryServer = function(callback){ 
	var msg = {};
    msg.cmd = "query";
    this.invoke(msg, function (msg) {
    	    callback(msg.body); 
    });
}

var MqClient = MessageClient;
 
function fullAddress(serverAddress){
	var scheme = "ws://"
	if(serverAddress.sslEnabled){
		scheme = "wss://" 
	}
	return scheme + serverAddress.address;
}

function BrokerRouteTable() {
    this.serverTable = {}; //Server Address ==> ServerInfo
    this.topicTable = {};  //Topic ==> Server List(topic span across ZbusServers)
    this.votesTable = {};  //Server Address ==> Voted tracker list
}

BrokerRouteTable.prototype.updateVotes = function (trackerInfo) {
    var trackedServerList = trackerInfo.trackedServerList;
    var trackerFullAddr = fullAddress(trackerInfo.serverAddress); 
    for (var i in trackedServerList) {
        var fullAddr = fullAddress(trackedServerList[i]);
        var votedTrackerSet = this.votesTable[fullAddr];
        if (!votedTrackerSet) {
            votedTrackerSet = new Set();
            this.votesTable[fullAddr] = votedTrackerSet;
        }
        votedTrackerSet.add(trackerFullAddr);
    }

    var toRemove = [];
    for (var fullAddr in this.votesTable) {
        var votedByThisTracker = false;
        for (var i in trackedServerList) {
            var trackedFullAddr = fullAddress(trackedServerList[i]);
            if (fullAddr == trackedFullAddr) {
                votedByThisTracker = true;
                break;
            }
        }
        var votedTrackerSet = this.votesTable[fullAddr];
        if (votedTrackerSet.has(trackerFullAddr) && !votedByThisTracker) {
            votedTrackerSet.delete(trackedFullAddr);
        }
        if (this.canRemove(votedTrackerSet)) {
            toRemove.push(fullAddr);
        }
    }
    for (var i in toRemove) {
        var fullAddr = toRemove[i];
        delete this.serverTable[fullAddr];
    }
    this.rebuildTopicTable();

    return toRemove; 
}

BrokerRouteTable.prototype.updateServer = function (serverInfo) {
    this.rebuildTopicTable(serverInfo);
}

BrokerRouteTable.prototype.removeServer = function (serverAddress) {
    var fullAddr = fullAddress(serverAddress);
    var votedTrackerSet = this.votesTable[fullAddr];
    if (this.canRemove(votedTrackerSet)) {
        delete this.serverTable[fullAddr];
        this.rebuildTopicTable();
    } 
}
  
BrokerRouteTable.prototype.canRemove = function (votedTrackerSet) {
    return !votedTrackerSet ||votedTrackerSet.size == 0;
}

BrokerRouteTable.prototype.rebuildTopicTable = function (serverInfo) {
    if (serverInfo) {
        var fullAddr = fullAddress(serverInfo.serverAddress);
        this.serverTable[fullAddr] = serverInfo;
    }
    this.topicTable = {};
    for (var fullAddr in this.serverTable) {
        var serverInfo = this.serverTable[fullAddr];

        var serverTopicTable = serverInfo.topicTable;
        for (var topic in serverTopicTable) {
            var serverTopicInfo = serverTopicTable[topic];
            if (!(topic in this.topicTable)) {
                this.topicTable[topic] = [serverTopicInfo];
            } else {
                this.topicTable[topic].push(serverTopicInfo);
            }
        }
    }
}


function Broker(trackerAddress) {  
	this.trackerSubscribers = {}; 
    this.clientTable = {};
    this.routeTable = new BrokerRouteTable();

    this.onServerJoin = null;
    this.onServerLeave = null;
    this.onServerUpdated = null;
    
    if(trackerAddress){
    	    this.addTracker(trackerAddress);
    }
} 

Broker.prototype.addServer = function(serverAddress) { 
	var fullAddr = fullAddress(serverAddress);
	var client = this.clientTable[fullAddr];
    var broker = this;
    
    if (client != null) {
        client.queryServer(function (serverInfo) {
            broker.routeTable.updateServer(serverInfo);
            if (broker.onServerUpdated != null) {
                broker.onServerUpdated(serverInfo);
            }
        });
        return;
    } 

    client = new MqClient(fullAddr);

    	client.onDisconnected(function (event) {
        console.log("Disconnected from " + fullAddr);
        client.close(); 
        delete broker.clientTable[fullAddr] 
        broker.routeTable.removeServer(serverAddress);

        if (broker.onServerLeave != null) {
            broker.onServerLeave(serverAddress);
        }

        if (broker.onServerUpdated != null) {
            broker.onServerUpdated();
        }
    });

    client.connect(function (event) {
        broker.clientTable[fullAddr] = client;
        client.queryServer(function(serverInfo) {
            broker.routeTable.updateServer(serverInfo);
            if (broker.onServerJoin != null) {
                broker.onServerJoin(serverInfo);
            }
            if (broker.onServerUpdated != null) {
                broker.onServerUpdated(serverInfo);
            }
        }); 
    });  
}

Broker.prototype.addTracker = function (trackerAddress) {
    var fullAddr = fullAddress(trackerAddress);
    if (fullAddr in this.trackerSubscribers) {
        return;
    }
    var client = new MessageClient(fullAddr);
    this.trackerSubscribers[fullAddr] = client;

    var broker = this;

    client.onMessage(function (msg) {
        var trackerInfo = msg.body; 
        var serverAddressList = trackerInfo.trackedServerList;
        broker.routeTable.updateVotes(trackerInfo);
        for (var i in serverAddressList) {
            broker.addServer(serverAddressList[i]);
        }
    });

    client.connect(function (event) {
        var msg = {};
        msg.cmd = Protocol.TRACK_SUB;
        client.sendMessage(msg);
    });
}



//////////////////////////////////////////////////////////

function MqAdmin(broker, topic){
	this.broker = broker;
	this.topic = topic; 
	this.flag = 0;
} 

MqAdmin.prototype.declareTopic = function(callback){ 
    var msg = {};
    msg.cmd = Protocol.DECLARE; 
    msg.topic = this.topic; 
    msg.flag = this.flag; 
    
    this.broker.invoke(msg, callback);
};


function Producer(broker, topic){
	MqAdmin.call(this, broker, topic); 
}


inherits(Producer, MqAdmin)

Producer.prototype.publish = function(msg, callback){
    msg.cmd = Protocol.PRODUCE;
    msg.topic = this.topic;
    this.broker.invoke(msg, callback);
};


function Consumer(broker, topic){
	MqAdmin.call(this, broker, topic); 
}

inherits(Consumer, MqAdmin);

Consumer.prototype.take = function(callback){
    var msg = {};
    msg.cmd = Protocol.CONSUME;
    msg.topic = this.topic;  

    var consumer = this;
    this.broker.invoke(msg, function(res){
        if(res.status == 404){
            consumer.declareTopic(function(res){ 
            	if(res.status == 200){
            		console.log(consumer.topic + " created");
            	}
                consumer.take(callback);
            });
            return;
        } 
        if(res.status == 200){
        	var originUrl = res.originUrl
        	var id = res.originId;
        	
        	delete res.originUrl
        	delete res.originId 
        	if(typeof originUrl == "undefined"){
        		originUrl = "/";
        	}  
        	
        	res.id = id;  
        	res.url = originUrl; 
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
    msg.cmd = Protocol.Route;
    msg.ack = false;
    this.broker.invoke(msg);
}; 


