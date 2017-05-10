#encoding=utf8  
import uuid, socket
import time, json
import logging.config, os 
import threading

class Protocol:  
    VERSION_VALUE = "0.8.0"       #start from 0.8.0 
    #############Command Values############
    #MQ Produce/Consume
    PRODUCE = "produce"
    CONSUME = "consume"
    RPC = "rpc"
    ROUTE = "route"     #route back message to sender, designed for RPC
    
    #Topic/ConsumeGroup control
    DECLARE = "declare"
    QUERY = "query"
    REMOVE = "remove"
    EMPTY = "empty"
    
    #Tracker
    TRACK_PUB = "track_pub"
    TRACK_SUB = "track_sub"
    
    COMMAND = "cmd"
    TOPIC = "topic";
    TOPIC_FLAG = "topic_flag"
    TAG = "tag"
    OFFSET = "offset"
    
    CONSUME_GROUP = "consume_group"
    CONSUME_GROUP_COPY_FROM = "consume_group_copy_from"
    CONSUME_START_OFFSET = "consume_start_offset"
    CONSUME_START_MSGID = "consume_start_msgid"
    CONSUME_START_TIME = "consume_start_time"
    CONSUME_WINDOW = "consume_window"
    CONSUME_FILTER_TAG = "consume_filter_tag"
    CONSUME_GROUP_FLAG = "consume_group_flag"
    
    SENDER = "sender"
    RECVER = "recver"
    ID = "id"
    
    SERVER = "server"
    ACK = "ack"
    ENCODING = "encoding"
    
    ORIGIN_ID = "origin_id"         #original id, TODO compatible issue: rawid
    ORIGIN_URL = "origin_url"       #original URL  
    ORIGIN_STATUS = "origin_status" #original Status  TODO compatible issue: reply_code
    
    #Security 
    TOKEN = "token"
    
    
    ############Flag values############    
    FLAG_PAUSE = 1 << 0
    FLAG_RPC = 1 << 1
    FLAG_EXCLUSIVE = 1 << 2
    FLAG_DELETE_ON_EXIT = 1 << 3


###################################################################################   
try:
    log_file = 'log.conf'   
    if os.path.exists(log_file):
        logging.config.fileConfig(log_file)
    else:
        import os.path
        log_dir = os.path.dirname(os.path.realpath(__file__))
        log_file = os.path.join(log_dir, 'log.conf')
        logging.config.fileConfig(log_file)
except: 
    logging.basicConfig(format='%(asctime)s - %(filename)s-%(lineno)s - %(levelname)s - %(message)s')


    
class Message(dict):
    http_status = { 
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
        "500": "Internal Server Error",
    }
    reserved_keys = set(['status', 'method', 'url', 'body'])
    
    def __init__(self, opt = None):
        self.body = None
        if opt and isinstance(opt, dict):
            for k in opt:
                self[k] = opt[k]
        
    def __getattr__(self, name):
        if name in self:
            return self[name]
        else:
            return None

    def __setattr__(self, name, value):
        self[name] = value

    def __delattr__(self, name):
        if name in self:
            del self[name] 
            
    def __getitem__(self, key):
        if key not in self:
            return None
        return dict.__getitem__(self, key)


def msg_encode(msg):
    if not isinstance(msg, dict):
        raise ValueError('%s must be dict type'%msg)
    if not isinstance(msg, Message):
        msg = Message(msg)
     
    res = bytearray() 
    if msg.status is not None:
        desc = Message.http_status.get('%s'%msg.status)
        if desc is None: desc = b"Unknown Status"
        res += bytes("HTTP/1.1 %s %s\r\n"%(msg.status, desc), 'utf8')  
    else:
        m = msg.method
        if not m: 
            m = 'GET'
        url = msg.url
        if not url:
            url = '/'
        res += bytes("%s %s HTTP/1.1\r\n"%(m, url), 'utf8') 
        
    body_len = 0
    if msg.body is not None:
        body_len = len(msg.body) 
        if not isinstance(msg.body, (bytes, bytearray)):
            if not msg['content-type']:
                msg['content-type'] = 'text/plain'
        
    for k in msg:
        if k.lower() in Message.reserved_keys: continue
        if msg[k] is None: continue
        res += bytes('%s: %s\r\n'%(k,msg[k]), 'utf8')
    len_key = 'content-length'
    if len_key not in msg:
        res += bytes('%s: %s\r\n'%(len_key, body_len), 'utf8')
    
    res += bytes('\r\n', 'utf8')
    
    body_encoding = 'utf8'
    if msg.encoding:
        body_encoding = msg.encoding
    if msg.body is not None:
        if isinstance(msg.body, (bytes, bytearray)):
            res += msg.body
        else:
            res += bytes(str(msg.body), body_encoding)
    return res


 
def find_header_end(buf, start=0):
    i = start
    end = len(buf)
    while i+3<end:
        if buf[i]==13 and buf[i+1]==10 and buf[i+2]==13 and buf[i+3]==10:
            return i+3
        i += 1
    return -1 
     
def decode_headers(buf):
    msg = Message()
    buf = buf.decode('utf8')
    lines = buf.splitlines() 
    meta = lines[0]
    blocks = meta.split()
    if meta.startswith('HTTP'):
        msg.status = blocks[1] 
    else: 
        msg.method = blocks[0]
        if len(blocks) > 1:
            msg.url = blocks[1] 
    
    for i in range(1,len(lines)):
        line = lines[i] 
        if len(line) == 0: continue
        try:
            p = line.index(':') 
            key = str(line[0:p]).strip()
            val = str(line[p+1:]).strip()
            msg[key] = val
        except Exception as e:
            logging.error(e) 
            
    return msg
  
def msg_decode(buf, start=0): 
    p = find_header_end(buf, start)
    if p < 0:
        return (None, start) 
    head = buf[start: p]
    msg = decode_headers(head) 
    if msg is None:
        return (None, start)
    p += 1 #new start

    body_len = msg['content-length']
    if body_len is None: 
        return (msg, p)
    body_len = int(body_len)
    if len(buf)-p < body_len:
        return (None, start)
    
    msg.body = buf[p: p+body_len]
    content_type = msg['content-type']
    encoding = 'utf8'
    if msg.encoding: encoding = msg.encoding
    if content_type:
        if str(content_type).startswith('text') or str(content_type) == 'application/json': 
            msg.body = msg.body.decode(encoding) 
            
    return (msg,p+body_len) 

       
        
class MessageClient(object):
    log = logging.getLogger(__name__)
    def __init__(self, address='localhost:15555'):  
        self.lock = threading.Lock()
        bb = address.split(':')
        self.host = bb[0]
        self.port = 80
        if(len(bb)>1): 
            self.port = int(bb[1]);
        
        self.read_buf = bytearray() 
        self.sock = None  
        self.id = uuid.uuid4()
        self.auto_reconnect = True
        self.reconnect_interval = 3 #3 seconds
        
        self.result_table = {}  
        
        self.on_connected = None
        self.on_disconnected = None
        self.on_message = None
    
    def close(self):
        if self.sock: 
            with self.lock:  
                self._close()  

    
    def invoke(self, msg, timeout=10): 
        with self.lock:  
            msgid = self._send(msg, timeout)
            return self._recv(msgid, timeout)
    
    def send(self, msg, timeout=3):
        with self.lock:
            return self._send(msg, timeout) 
     
    def recv(self, msgid=None, timeout=3):
        with self.lock:
            return self._recv(msgid, timeout)   
     

    def connect(self):  
        with self.lock:
            if self.sock:
                self.sock.close()
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.log.info('Trying connect to (%s:%s)'%(self.host, self.port))
            self.sock.connect( (self.host, self.port) )
            self.log.info('Connected to (%s:%s)'%(self.host, self.port)) 
            
        if self.on_connected:
            self.on_connected()
        
        self.read_buf = bytearray() 
    
    def _send(self, msg, timeout=10):
        msgid = msg.id
        if not msgid:
            msgid = msg.id = str(uuid.uuid4())
        
        self.log.debug('Request: %s'%msg)  
        #self._connect_if_need()
        self.sock.sendall(msg_encode(msg))  
        return msgid 
    
    def _recv(self, msgid=None, timeout=3):
        if not msgid and len(self.result_table)>0:
            try:
                self.result_table.popitem()[1]
            except:
                pass
            
        if msgid in self.result_table:
            return self.result_table[msgid]  
        
        #self._connect_if_need()
        self.sock.settimeout(timeout)    
        while True: 
            buf = self.sock.recv(1024)   
            self.read_buf += buf
            idx = 0
            while True:
                msg, idx = msg_decode(self.read_buf, idx)
                if msg is None: 
                    if idx != 0:
                        self.read_buf = self.read_buf[idx:]
                    break
                
                self.read_buf = self.read_buf[idx:]   
                
                if msgid:
                    if msg.id != msgid:
                        self.result_table[msg.id] = msg
                        continue 
                
                self.log.debug('Result: %s'%msg) 
                return msg   
    
    def _close(self):
        if self.sock:  
            self.sock.close()
            self.sock = None
            self.read_buf = bytearray() 
    
    def start(self, recv_timeout=3):  
        def serve(): 
            while True: 
                try: 
                    self.connect()
                    break
                except socket.error as e:
                    self.log.warn(e) 
                    time.sleep(self.reconnect_interval)
                    
            while True: 
                try: 
                    msg = self.recv(None, recv_timeout)
                    if msg and self.on_message:
                        self.on_message(msg)
                except socket.timeout:
                    continue
                except socket.error as e:
                    self.log.warn(e)
                    if self.on_disconnected:
                        self.on_disconnected()
                    while True:
                        try:
                            self.connect()
                            break
                        except Exception as e:
                            self.log.warn(e)
                            self.sock = None
                            time.sleep(self.reconnect_interval)
                
        self._thread = threading.Thread(target=serve)  
        self._thread.start()


class MqClient(MessageClient):
    def __init__(self, address='localhost:15555'):
        MessageClient.__init__(self, address)
        self.token = None
    
    def _invoke_void(self, cmd, msg, timeout=3):    
        msg.cmd = cmd
        msg.token = self.token
        
        res = self.invoke(msg, timeout)
        if res.status != "200":
            encoding = res.encoding
            if not encoding: encoding = 'utf8'
            raise Exception(res.body.decode(encoding))
    
    def _invoke_json(self, cmd, msg, timeout=3):    
        msg.cmd = cmd
        msg.token = self.token
        
        res = self.invoke(msg, timeout)
        if res.status != "200":
            encoding = res.encoding
            if not encoding: encoding = 'utf8'
            raise Exception(res.body.decode(encoding))
        return json.loads(res.body, encoding=res.encoding) 
    
    
    def produce(self, msg, timeout=3):
        self._invoke_void(Protocol.PRODUCE, msg, timeout)
        
    def consume(self, topic, ctrl=None, timeout=3):
        msg = Message()
        msg.topic = topic
        msg.token = self.token 
        if isinstance(ctrl, str):
            msg.consume_group = ctrl
        if isinstance(ctrl, (dict, Message)):
            for k in ctrl:
                msg[k] = ctrl[k]
        msg.cmd = Protocol.CONSUME 
        return self.invoke(msg, timeout)
    
    def query_server(self, timeout=3):
        return self._invoke_json(Protocol.QUERY, Message(), timeout)
    
    def query_topic(self, topic, timeout=3):
        msg = Message();
        msg.topic = topic
        return self._invoke_json(Protocol.QUERY, msg, timeout)    
    
    def query_group(self, topic, group, timeout=3):
        msg = Message();
        msg.topic = topic
        msg.consume_group = group
        return self._invoke_json(Protocol.QUERY, msg, timeout)     
         
    def declare_topic(self, topic, topic_flag=None, timeout=3):
        msg = Message();
        msg.topic = topic
        if topic_flag:
            msg.topic_flag = topic_flag
        return self._invoke_json(Protocol.DECLARE, msg, timeout)  
     
    def declare_group(self, topic, group, timeout=3):
        msg = Message();
        msg.topic = topic
        msg.consume_group = group
        return self._invoke_json(Protocol.DECLARE, msg, timeout) 
    
    def remove_topic(self, topic, timeout=3):
        msg = Message();
        msg.topic = topic  
        self._invoke_void(Protocol.REMOVE, msg, timeout)   
        
    def remove_group(self, topic, group, timeout=3):
        msg = Message();
        msg.topic = topic
        msg.consume_group = group 
        self._invoke_void(Protocol.REMOVE, msg, timeout)      
    
    def empty_topic(self, topic, timeout=3):
        msg = Message();
        msg.topic = topic  
        self._invoke_void(Protocol.EMPTY, msg, timeout)   
        
    def empty_group(self, topic, group, timeout=3):
        msg = Message();
        msg.topic = topic
        msg.consume_group = group 
        self._invoke_void(Protocol.EMPTY, msg, timeout)            

class BrokerRouteTable:
    pass            

class Broker:
    def __init__(self):
        pass
    
    def add_tracker(self, tracker_address, cert_file=None):
        pass
    
    def add_server(self, server_address, cert_file=None):
        pass
 
class Producer:
    pass

class Consumer:
    pass

class RpcInvoker:
    pass

class RpcProcessor:
    pass 
          
__all__ = [
    Message, MessageClient, MqClient, Broker, Producer, Consumer, RpcInvoker, RpcProcessor
]    