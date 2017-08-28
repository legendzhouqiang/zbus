class Protocol{ }
//!!!Javascript message control: xxx_yyy = xxx-yyy (underscore equals dash), both support
//MQ Produce/Consume
Protocol.PRODUCE = "produce";
Protocol.CONSUME = "consume"; 
Protocol.ROUTE   = "route";     //route back message to sender, designed for RPC

//Topic/ConsumeGroup control
Protocol.DECLARE = "create_mq";  //NEW: declare
Protocol.QUERY   = "query_mq";
Protocol.REMOVE  = "remove_mq"; 
 

Protocol.COMMAND    = "cmd";
Protocol.TOPIC      = "mq";  

Protocol.SENDER = "sender";
Protocol.RECVER = "recver";
Protocol.ID     = "id";

Protocol.ACK      = "ack";
Protocol.ENCODING = "encoding";

Protocol.ORIGIN_ID     = "rawid";    
Protocol.ORIGIN_URL    = "origin_url";    
Protocol.ORIGIN_STATUS = "reply_code";  

//Security 
Protocol.TOKEN = "token"; 

Protocol.HEARTBEAT = 'heartbeat';
  

////////////////////////////////////////////////////////////
function hashSize(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
}

function randomKey (obj) {
    var keys = Object.keys(obj)
    return keys[keys.length * Math.random() << 0];
}

function clone(obj) {
    if (null == obj || "object" != typeof obj) return obj;
    var copy = obj.constructor();
    for (var attr in obj) {
        if (obj.hasOwnProperty(attr)) copy[attr] = obj[attr];
    }
    return copy;
}

function uuid() {
    //http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

String.prototype.format = function () {
    var args = arguments;
    return this.replace(/{(\d+)}/g, function (match, number) {
        return typeof args[number] != 'undefined' ? args[number] : match;
    });
}

Date.prototype.format = function (fmt) {
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

function camel2underscore(key) {
    return key.replace(/\.?([A-Z])/g, function (x, y) { return "_" + y.toLowerCase() }).replace(/^_/, "");
}

function underscore2camel(key) {
    return key.replace(/_([a-z])/g, function (g) { return g[1].toUpperCase(); });
} 

var HttpStatus = {
    200: "OK",
    201: "Created",
    202: "Accepted",
    204: "No Content",
    206: "Partial Content",
    301: "Moved Permanently",
    304: "Not Modified",
    400: "Bad Request",
    401: "Unauthorized",
    403: "Forbidden",
    404: "Not Found",
    405: "Method Not Allowed",
    416: "Requested Range Not Satisfiable",
    500: "Internal Server Error"
};

function asleep(ms){
    return new Promise(resolve=>{
        setTimeout(resolve, ms);
    });
}
/////////////////////////logging////////////////////////////

class Logger { 
  constructor(level){
    this.level = level;
    if(!level){
      this.level = Logger.DEBUG;
    }

    this.DEBUG = 0;
    this.INFO = 1;
    this.WARN = 2;
    this.ERROR = 3;  
  } 

  debug() { this._log(this.DEBUG, ...arguments); } 
  info() { this._log(this.INFO, ...arguments); } 
  warn() { this._log(this.WARN, ...arguments); } 
  error() { this._log(this.ERROR, ...arguments); } 
  
  log(level) { this._log(level, ...Array.prototype.slice.call(arguments, 1)); } 

  _log(level) {
    if(level<this.level) return;
    var args = Array.prototype.slice.call(arguments, 1);
    var levelString = "UNKNOWN";
    if(level == this.DEBUG){
        levelString = "DEBUG";
    } 
    if(level == this.INFO){
        levelString = "INFO";
    }
    if(level == this.WARN){
        levelString = "WARN";
    }
    if(level == this.ERROR){
        levelString = "ERROR";
    } 
    args.splice(0, 0, "["+levelString+"]");
    console.log(new Date().format("yyyy/MM/dd hh:mm:ss.S"), ...this._format(args));
  }

  _format(args) {
    args = Array.prototype.slice.call(args) 
    var stackInfo = this._getStackInfo(2) //info => _log => _format  back 2

    if (stackInfo) { 
      var calleeStr = stackInfo.relativePath + ':' + stackInfo.line;
      if (typeof (args[0]) === 'string') {
        args[0] = calleeStr + ' ' + args[0]
      } else {
        args.unshift(calleeStr)
      }
    } 
    return args
  }

  _getStackInfo(stackIndex) { 
    // get all file, method, and line numbers
    var stacklist = (new Error()).stack.split('\n').slice(3)

    // stack trace format: http://code.google.com/p/v8/wiki/JavaScriptStackTraceApi
    // do not remove the regex expresses to outside of this method (due to a BUG in node.js)
    var stackReg = /at\s+(.*)\s+\((.*):(\d*):(\d*)\)/gi
    var stackReg2 = /at\s+()(.*):(\d*):(\d*)/gi

    var s = stacklist[stackIndex] || stacklist[0]
    var sp = stackReg.exec(s) || stackReg2.exec(s)

    if (sp && sp.length === 5) {
      return {
        method: sp[1],
        relativePath: sp[2].replace(/^.*[\\\/]/, ''),
        line: sp[3],
        pos: sp[4],
        file: sp[2],
        stack: stacklist.join('\n')
      }
    }
  }
}

 
var logger = new Logger(Logger.INFO);  //should export

//////////////////////////////////////////////////////////// 
var __NODEJS__ = typeof module !== 'undefined' && module.exports;

if (__NODEJS__) {

var Events = require('events');
var Buffer = require("buffer").Buffer;
var Socket = require("net");
var inherits = require("util").inherits;
var tls = require("tls");
var fs = require("fs");
    
function string2Uint8Array(str, encoding) {
    var buf = Buffer.from(str, encoding); 
    var data = new Uint8Array(buf.length);
    for (var i = 0; i < buf.length; ++i) {
        data[i] = buf[i];
    }
    return data;
}

function uint8Array2String(data, encoding) {
    var buf = Buffer.from(data);
	return buf.toString(encoding);
} 
    
} else { //WebSocket
	
function inherits(ctor, superCtor) {
	ctor.super_ = superCtor;
	ctor.prototype = Object.create(superCtor.prototype, {
	    constructor: {
	        value: ctor,
	        enumerable: false,
	        writable: true,
	        configurable: true
	    }
	});
};
	
	
function string2Uint8Array(str, encoding) {
	var encoder = new TextEncoder(encoding);
	return encoder.encode(str);
}

function uint8Array2String(buf, encoding) {
	var decoder = new TextDecoder(encoding);
	return decoder.decode(buf);
}

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
    if (!encoding) encoding = "utf8"; 

    var body = msg.body;
    var contentType = msg["content-type"];
    if (body) {
        if (body instanceof ArrayBuffer) {
            body = new Uint8Array(body);
        } else if (body instanceof Uint8Array || body instanceof Int8Array) {
            //no need to handle
        } else {
            if (typeof body != 'string') {
                body = JSON.stringify(body);
                contentType = "application/json";
            } 
            body = string2Uint8Array(body, encoding);
        }
    } else {
        body = new Uint8Array(0);
    }
    msg["content-length"] = body.byteLength;
    msg["content-type"] = contentType;

    var nonHeaders = { 'status': true, 'method': true, 'url': true, 'body': true }
    var line = ""
    if (msg.status) {
        var desc = HttpStatus[msg.status];
        if (!desc) desc = "Unknown Status";
        line = "HTTP/1.1 {0} {1}\r\n".format(msg.status, desc);
    } else {
        var method = msg.method;
        if (!method) method = "GET";
        var url = msg.url;
        if (!url) url = "/";
        line = "{0} {1} HTTP/1.1\r\n".format(method, url);
    }

    headers += line;
    for (var key in msg) {
        if (key in nonHeaders) continue;
        var val = msg[key];
        if (typeof val == 'undefined') continue;
        line = "{0}: {1}\r\n".format(camel2underscore(key), msg[key]);
        headers += line;
    }
    headers += "\r\n";

    delete msg["content-length"]; //clear
    delete msg["content-type"]

    var headerBuffer = string2Uint8Array(headers, encoding);
    var headerLen = headerBuffer.byteLength;
    //merge header and body
    var buffer = new Uint8Array(headerBuffer.byteLength + body.byteLength);  
    for (var i = 0; i < headerBuffer.byteLength; i++) {
        buffer[i] = headerBuffer[i];
    }

    for (var i = 0; i < body.byteLength; i++) {
        buffer[headerLen + i] = body[i];
    }
    return buffer;
};
 
function httpDecode(data) {
    var encoding = "utf8";
    if (typeof data == "string") {
        data = string2Uint8Array(data, encoding);
    } else if (data instanceof Uint8Array || data instanceof Int8Array) {
        //ignore
    } else if (data instanceof ArrayBuffer) {
        data = new Uint8Array(data);
    } else {
        //not support type
        return null;
    }
    var i = 0, pos = -1;
    var CR = 13, NL = 10;
    while (i + 3 < data.byteLength) {
        if (data[i] == CR && data[i + 1] == NL && data[i + 2] == CR && data[i + 3] == NL) {
            pos = i;
            break;
        }
        i++;
    }
    if (pos == -1) return null;

    var str = uint8Array2String(data.slice(0, pos), encoding);

    var blocks = str.split("\r\n");
    var lines = [];
    for (var i in blocks) {
        var line = blocks[i];
        if (line == '') continue;
        lines.push(line);
    }

    var msg = {};
    //parse first line 
    var bb = lines[0].split(" ");
    if (bb[0].toUpperCase().startsWith("HTTP")) {
        msg.status = parseInt(bb[1]);
    } else {
        msg.method = bb[0];
        msg.url = bb[1];
    }
    var typeKey = "content-type";
    var typeVal = "text/plain";
    var lenKey = "content-length";
    var lenVal = 0;

    for (var i = 1; i < lines.length; i++) {
        var line = lines[i];
        var p = line.indexOf(":");
        if (p == -1) continue;
        var key = line.substring(0, p).trim().toLowerCase();
        var val = line.substring(p + 1).trim();
        if (key == lenKey) {
            lenVal = parseInt(val);
            continue;
        }
        if (key == typeKey) {
            typeVal = val;
            continue;
        }

        key = underscore2camel(key);
        msg[key] = val;
    }
    if (pos + 4 + lenVal > data.byteLength) {
        return null;
    }
    //mark consumed Length
    data.consumedLength = pos + 4 + lenVal;

    var encoding = msg.encoding;
    if (!encoding) encoding = "utf8"; 
    var bodyData = data.slice(pos + 4, pos + 4 + lenVal);
    if (typeVal == "text/html" || typeVal == "text/plain") {
        msg.body = uint8Array2String(bodyData, encoding);
    } else if (typeVal == "application/json") {
        var bodyString = uint8Array2String(bodyData, encoding);
        msg.body = JSON.parse(bodyString);
    } else {
        msg.body = bodyData;
    }

    return msg;
}


function Ticket(reqMsg, resolve) {
    this.id = uuid();
    reqMsg.id = this.id;

    this.request = reqMsg;
    this.resolve = resolve;  
} 

function addressKey(serverAddress) {
    var scheme = ""
    if (serverAddress.sslEnabled) {
        scheme = "[SSL]"
    }
    return scheme + serverAddress.address;
}

function normalizeAddress(serverAddress, certFile) {
    if (typeof (serverAddress) == 'string') {
        var sslEnabled = false;
        if (certFile) sslEnabled = true;

        serverAddress = serverAddress.trim();
        if (serverAddress.startsWith("ws://")) {
            serverAddress = serverAddress.substring(5);
        }
        if (serverAddress.startsWith("wss://")) {
            serverAddress = serverAddress.substring(6);
            sslEnabled = true;
        }

        serverAddress = {
            address: serverAddress,
            sslEnabled: sslEnabled,
        }; 
    }
    return serverAddress; 
}

function containsAddress(serverAddressArray, serverAddress) {
    for (var i in serverAddressArray) {
        var addr = serverAddressArray[i];
        if (addr.address == serverAddress.address && addr.sslEnabled == serverAddress.sslEnabled) {
            return true;
        }
    }
    return false;
}

function MqClient(serverAddress, certFile) { 
    if (__NODEJS__) {
        Events.EventEmitter.call(this);
    }
    serverAddress = normalizeAddress(serverAddress, certFile); 
    this.serverAddress = serverAddress;
    this.certFile = certFile;

    var address = this.serverAddress.address;
    var p = address.indexOf(':');
    if (p == -1) {
        this.serverHost = address.trim();
        this.serverPort = 80;
    } else {
        this.serverHost = address.substring(0, p).trim();
        this.serverPort = parseInt(address.substring(p + 1).trim());
    }

    this.autoReconnect = true;
    this.reconnectInterval = 3000;
    this.heartbeatInterval = 60*1000; //60 seconds
    this.ticketTable = {};
    this.onMessage = null;
    this.onConnected = null;
    this.onDisconnected = null; 
    
    this.token = null;

    this.readBuf = new Uint8Array(0); //only used when NODEJS 
} 

if (__NODEJS__) {
	
inherits(MqClient, Events.EventEmitter); 

MqClient.prototype.connect = function (connectedHandler) { 
    if(this.socket && this.connectPromise) {
        return this.connectPromise;
    }
    this.onConnected = connectedHandler;
    //should handle failure 
    var connectSuccess;
    var connectFailure;
    this.connectPromise = new Promise((resolve, reject) => {
        connectSuccess = resolve;
        connectFailure = reject;
    }); 

    try {
        logger.debug("Trying to connect: " + this.serverHost + ":" + this.serverPort);
        var certFile = this.certFile;
        if(this.serverAddress.sslEnabled) { 
            logger.debug("cert:" + this.certFile);
            this.socket = tls.connect(this.serverPort, {  
                cert: fs.readFileSync(certFile),  
                ca: fs.readFileSync(certFile),
            });
        } else {
            this.socket = Socket.connect({ host: this.serverHost, port: this.serverPort }); 
        }  
    } catch (e){
        connectFailure(e);
        return this.connectPromise;
    }
    

    var client = this;
    this.socket.on("connect", function () {
        logger.debug("MqClient connected: " + client.serverHost + ":" + client.serverPort);
        if (connectSuccess) {
            connectSuccess();
        }
        if(client.onConnected){
            client.onConnected(client);
        }
       
        client.heartbeat = setInterval(function () { 
            var msg = {cmd: Protocol.HEARTBEAT};
            try{ client.send(msg); }catch(e){ logger.warn(e); }
        }, client.heartbeatInterval);
    }); 

    this.socket.on("error", function (error) {
        client.emit("error", error);
    });

    this.socket.on("close", function () {
        this.connectPromise = null;
        client.socket.destroy();
        client.socket = null;
        clearInterval(client.heartbeat);
        client.removeAllListeners();

        if (client.onDisconnected) {
            client.onDisconnected(); 
        }  

        if (client.autoReconnect) {
            logger.debug("Trying to recconnect: " + client.serverHost + ":" + client.serverPort); 
            
            client.connectTrying = setTimeout(function () {
                client.connect(connectedHandler);
            }, client.reconnectInterval);
        }
    });

    client.on("error", function (error) {  
        logger.error(error); 
    });

    this.socket.on("data", function (data) { 
        var encoding = "utf8";
        if (typeof data == "string") {
            data = string2Uint8Array(data, encoding);
        } else if (data instanceof Uint8Array || data instanceof Int8Array) {
            //ignore
        } else if (data instanceof ArrayBuffer) {
            data = new Uint8Array(data);
        } else {
            throw "Invalid data type: " + data;
        }
        //concate data
        var bufLen = client.readBuf.byteLength;
        var readBuf = new Uint8Array(bufLen + data.byteLength);
        for (var i = 0; i < bufLen; i++) {
            readBuf[i] = client.readBuf[i];
        }
        for (var i = 0; i < data.byteLength; i++) {
            readBuf[i + bufLen] = data[i];
        }
        client.readBuf = readBuf;

        while (true) {
            if (client.readBuf.consumedLength) {
                client.readBuf = client.readBuf.slice(client.readBuf.consumedLength);
            }
            var msg = httpDecode(client.readBuf);
            if (msg == null) break;
            var msgid = msg.id;
            var ticket = client.ticketTable[msgid];
            if (ticket) { 
                if(ticket.resolve){
                    ticket.resolve(msg);
                }
                delete client.ticketTable[msgid];
            } else if (client.onMessage) {
                client.onMessage(msg);
            }
        } 
    });

    return this.connectPromise;
}; 

MqClient.prototype.active = function () {
     return this.socket && !this.socket.destroyed;
}

MqClient.prototype.send = function (msg) {
    var buf = httpEncode(msg); 
    this.socket.write(Buffer.from(buf));
}


MqClient.prototype.close = function () {
    this.connectPromise = null;
    if (!this.socket) return;
    clearInterval(this.heartbeat);
    if(this.connectTrying){
        clearTimeout(this.connectTrying);
    } 
    this.socket.removeAllListeners("close");
    this.autoReconnect = false;
    this.socket.destroy();
    this.socket = null;
} 

} else { 
//WebSocket
	
MqClient.prototype.connect = function (connectedHandler) {
    if(this.socket && this.connectPromise) {
        return this.connectPromise;
    }
    logger.debug("Trying to connect to " + this.address()); 
    this.onConnected = connectedHandler; 

    //should handle failure 
    var connectSuccess;
    this.connectPromise = new Promise(resolve => {
        connectSuccess = resolve;
    });

    var WebSocket = window.WebSocket;
    if (!WebSocket) {
        WebSocket = window.MozWebSocket;
    }

    this.socket = new WebSocket(this.address());
    this.socket.binaryType = 'arraybuffer';
    var client = this;
    this.socket.onopen = function (event) {
        logger.debug("Connected to " + client.address());
        if (connectSuccess) {
            connectSuccess();
        }
        if(client.onConnected){
            client.onConnected(client);
        }

        client.heartbeat = setInterval(function () {
            var msg = {cmd: Protocol.HEARTBEAT};
            try{ client.send(msg); }catch(e){ logger.warn(e); }
        }, client.heartbeatInterval);
    };

    this.socket.onclose = function (event) {
        this.connectPromise = null;
        clearInterval(client.heartbeat);
        if (client.onDisconnected) {
            client.onDisconnected(); 
        }
        if (client.autoReconnect) {
            client.connectTrying = setTimeout(function () {
                try { client.connect(connectedHandler); } catch (e) { }//ignore
            }, client.reconnectInterval);
        }
    };

    this.socket.onmessage = function (event) {
        var msg = httpDecode(event.data);
        var msgid = msg.id;
        var ticket = client.ticketTable[msgid];
        if (ticket) {
            if(ticket.resolve){
                ticket.resolve(msg);
            }
            delete client.ticketTable[msgid];
        } else if (client.onMessage) {
            client.onMessage(msg);
        }  
    }

    this.socket.onerror = function (data) {
        logger.error("Error: " + data); 
    }
    return this.connectPromise;
}

MqClient.prototype.active = function () {
     return this.socket && this.socket.readyState == WebSocket.OPEN;
}

MqClient.prototype.send = function (msg) {
    var buf = httpEncode(msg);
    this.socket.send(buf);
}

MqClient.prototype.close = function () {
    this.connectPromise = null;
    clearInterval(this.heartbeat);
    if(this.connectTrying){
        clearTimeout(this.connectTrying);
    } 
    this.socket.onclose = function () { }
    this.autoReconnect = false;
    this.socket.close();
    this.socket = null;
}

MqClient.prototype.address = function () {
    if (this.serverAddress.sslEnabled) {
        return "wss://" + this.serverAddress.address;
    }
    return "ws://" + this.serverAddress.address;
}
 
}//End of __NODEJS___

///////////////////////////////commons for Node.JS and Browser///////////////////////////////////
 
MqClient.prototype.fork = function () {
    return new MqClient(this.serverAddress, this.certFile);
} 

MqClient.prototype.invoke = function (msg) {
    var client = this;
    var promise = new Promise((resolve, reject)=>{
        if (!client.active()) {
            reject(new Error("socket is not open, invalid"));
            return;
        } 
        var ticket = new Ticket(msg, resolve); 
        this.ticketTable[ticket.id] = ticket;
    });  

    this.send(msg); 
    return promise;
};

MqClient.prototype.invokeCmd = function (cmd, msg) {
    if (typeof (msg) == 'string') msg = { mq: msg };  //assume to be topic
    if (!msg) msg = {};

    if (this.token) msg.token = this.token;
    msg.cmd = cmd; 
    return this.invoke(msg);
}

MqClient.prototype.invokeObject = function (cmd, msg) {
    return this.invokeCmd(cmd, msg)
    .then((res)=>{ 
        if(res.status != 200){
            var error = new Error(res.body);
            error.msg = res;
            return {error: error};
        }
        return res.body;
    });
}
  
  
MqClient.prototype.route = function (msg) {
    msg.ack = false;
    if (this.token) msg.token = this.token;
    msg.cmd = Protocol.ROUTE; 
    if (msg.status) {
        msg[Protocol.ORIGIN_STATUS] = msg.status;
        delete msg.status;
    }
    this.send(msg);
}

MqClient.prototype.query = function (msg) { 
    return this.invokeObject(Protocol.QUERY, msg);
} 
MqClient.prototype.declare = function (msg) {
    if (typeof (msg) == 'string') msg = { mq_name: msg }; 
    return this.invokeObject(Protocol.DECLARE, msg);
} 
MqClient.prototype.remove = function (msg) {
    return this.invokeCmd(Protocol.REMOVE, msg);
} 

MqClient.prototype.produce = function (msg) {
    return this.invokeCmd(Protocol.PRODUCE, msg); 
}
MqClient.prototype.consume = function (msg) { 
    return this.invokeCmd(Protocol.CONSUME, msg);
} 

var Broker = MqClient;

////////////////////////////////////////////////////////// 
class MqAdmin {
    constructor(broker){
        this.broker = broker; 
    } 

    declare(msg) { 
        if(!this.broker.active()){
            return this.broker.connect()
            .then(()=>{
                return this.broker.declare(msg);
            });
        }
        return this.broker.declare(msg);
    }

    query (msg) {
        if(!this.broker.active()){
            return this.broker.connect()
            .then(()=>{
                return this.broker.query(msg);
            });
        }
        return this.broker.query(msg);
    }

    remove(msg) {
        if(!this.broker.active()){
            return this.broker.connect()
            .then(()=>{
                return this.broker.remove(msg);
            });
        }
        return this.broker.remove(msg);
    } 
}
 

class Producer extends(MqAdmin) {
    constructor(broker) {
        super(broker); 
    }  

    publish(msg) { 
        if(!this.broker.active()){
            return this.broker.connect()
            .then(()=>{
                return this.broker.produce(msg);
            });
        }
        return this.broker.produce(msg);
    } 
}

 
class Consumer extends MqAdmin {
    constructor(broker, consumeCtrl){
        super(broker);  
        this.connectionCount = 1;
        this.messageHandler = null;
        if (typeof consumeCtrl == 'string') {
            consumeCtrl = {mq: consumeCtrl} //string assumed to be topic, otherwise as key-value control header
        }
        this.consumeCtrl = clone(consumeCtrl); 
    }  

    start() { 
        if(!this.broker.active()){
            this.broker.connect()
            .then(()=>{
                this.consume(this.broker, this.consumeCtrl);
            });
            return;
        }
        this.consume(this.broker, this.consumeCtrl);
    }
     
    consume(client, consumeCtrl) { 
        var consumer = this; 
        client.consume(consumeCtrl)
        .then(res => {
            if(res.status == 404) {//Missing topic, to declare 
                var mqctrl = {mq_name: consumeCtrl.mq};
                return client.declare(mqctrl)
                .then(res=>{
                    if (res.error) { 
                        throw new Error("declare error: " + res.error);
                    } 
                    return consumer.consume(client, consumeCtrl);
                });
            } 

            var originUrl = res[Protocol.ORIGIN_URL];
            var id = res[Protocol.ORIGIN_ID]; 
            delete res[Protocol.ORIGIN_URL];
            delete res[Protocol.ORIGIN_ID];
            if (typeof originUrl == "undefined") originUrl = "/";  
            res.id = id;
            res.url = originUrl; 
            try {
                consumer.messageHandler(res, client);
            } finally {
                consumer.consume(client, consumeCtrl);
            } 
        }); 
    }
}

class RpcInvoker { 
    constructor(broker, mq, ctrl){ 
        this.broker = broker;
        this.prodcuer = new Producer(broker); 
        if (ctrl) {
            this.ctrl = clone(ctrl);
            this.ctrl.mq = mq;
        } else {
            this.ctrl = { mq: mq }
        } 

        var invoker = this; 
        return new Proxy(this, {
            get: function (target, name) {
                return name in target ? target[name] : invoker.proxyMethod(name);
            }
        });
    } 

     //{ method: xxx, params:[], module: xxx}
    invoke(){ 
        if (arguments.length < 1) {
            throw "Missing request parameter";
        }
        var req = arguments[0]; 
        if (typeof (req) == 'string') { 
            var params = [];
            var len = arguments.length; 
            for (var i = 1; i < len; i++) {
                params.push(arguments[i]);
            }
            req = {
                method: req,
                params: params,
            }
        } 
        return this.invokeMethod(req);
    } 
 
    invokeMethod(req) {
        var msg = clone(this.ctrl);
        msg.body = JSON.stringify(req);
        msg.ack = false;
        
        return this.prodcuer.publish(msg)
        .then(msg => {
            if (msg.status != 200) {
                throw msg.body;
            }
            var res = {};
            if(typeof(msg.body) == 'string'){
                res = JSON.parse(msg.body);
            } else {
                res = msg.body;
            }  
            if (res.error) {
                throw res.error;
            }
            return res.result;
        });
    } 

    proxyMethod(method) {
        var invoker = this;
        return function () {  
            var len = arguments.length; 
            var params = [];
            for (var i = 0; i < len; i++) {
                params.push(arguments[i]);
            }
            var req = {
                method: method,
                params: params,
            }  
            return invoker.invokeMethod(req);
        }
    }
}

class RpcProcessor {
    constructor(){
        this.methodTable = {} 
        var processor = this;
        this.messageHandler = function (msg, client) {
            var res = { error: null };  
            var resMsg = {
                id: msg.id,
                recver: msg.sender,
                topic: msg.topic,
                status: 200,
                body: res,
            }; 
            
            try { 
                var req = msg.body;
                if(typeof(req) == 'string') req = JSON.parse(req);  
                var key = processor._generateKey(req.module, req.method);
                var m = processor.methodTable[key];
                if(m) {
                    try {
                        res.result = m.method.apply(m.target, req.params); 
                    } catch (e) {
                        res.error = e; 
                    }  
                } else {
                    res.error = "method(" + key + ") not found";
                } 
            } catch (e) {
                res.error = e; 
            } finally {   
                try { client.route(resMsg); } catch (e) { }
            }
        }
    }

    _generateKey(moduleName, methodName){
        if (moduleName) return moduleName + ":" + methodName;
        return ":" + methodName;
    }

    addModule(serviceObj, moduleName) {
        if (typeof (serviceObj) == 'function') {
            var func = serviceObj;
            var key = this._generateKey(moduleName, func.name);
            if (key in this.methodTable) {
                logger.warn(key + " already exists, will be overriden");
            }
            this.methodTable[key] = { method: func, target: null};
            return;
        }

        for (var name in serviceObj) {
            var func = serviceObj[name];
            if (typeof (func) != 'function') continue;
            var key = this._generateKey(moduleName, name);
            if (key in this.methodTable) {
                logger.warn(key + " already exists, will be overriden");
            }
            this.methodTable[key] = { method: func, target: serviceObj };
        }
    }
}
  
if (__NODEJS__) {
    module.exports.Protocol = Protocol; 
    module.exports.MqClient = MqClient; 
    module.exports.Broker = Broker;
    module.exports.Producer = Producer;
    module.exports.Consumer = Consumer;
    module.exports.RpcInvoker = RpcInvoker;
    module.exports.RpcProcessor = RpcProcessor;
    module.exports.Logger = Logger;
    module.exports.logger = logger;
}

 