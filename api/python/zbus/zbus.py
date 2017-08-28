#encoding=utf8  
import logging, logging.config, os
import socket, uuid
import threading, time
import json
import inspect 
try:
    import Queue
except:
    import queue as Queue
    
try:
    log_file = 'log.conf' #优先搜索当前目录
    if os.path.exists(log_file):
        logging.config.fileConfig(log_file)
    else: #默认配置
        import os.path
        log_dir = os.path.dirname(os.path.realpath(__file__))
        log_file = os.path.join(log_dir, 'log.conf')
        logging.config.fileConfig(log_file)
except: 
    logging.basicConfig(format='%(asctime)s - %(filename)s-%(lineno)s - %(levelname)s - %(message)s')


class Meta:
    http_method = set(["GET", "POST", "HEAD", "PUT", "DELETE", "OPTIONS"])
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
    
    def __init__(self, meta = None): 
        self.status = None
        
        self.method = 'GET'
        self.uri = '/' 
        
        self.path = None
        self.params = None
        
        if meta is None or meta == '':
            return
        
        blocks = meta.split(None, 2)
        if meta.startswith('HTTP'):
            self.status = blocks[1]
            return
        
        self.method = blocks[0]
        if len(blocks) > 1:
            self.uri = blocks[1]
            self._decode_uri(self.uri)
        
    
    def __str__(self):   
        if self.status is not None:
            desc = Meta.http_status.get(self.status)
            if desc is None: desc = "Unknown Status"
            return "HTTP/1.1 %s %s"%(self.status, desc)
        return "%s %s HTTP/1.1"%(self.method, self.uri)
    
    
    def get_path(self):
        return self.path
    def get_method(self):
        return self.method
    def get_status(self):
        return self.status
    
    def get_param(self, key, default_value=None):
        if self.params is None:
            return default_value
        value = self.params.get(key)
        if value is None:
            value = default_value
        return value
    
    def _decode_uri(self, uri_str):
        uri_str = str(uri_str)
        idx = uri_str.find('?')
        if idx < 0 :
            self.path = uri_str
        else:
            self.path = uri_str[0:idx]
        if self.path[0] == '/':
            self.path = self.path[1:]
        
        if idx < 0: return
        
        param_str = uri_str[idx+1:]
        self.params = {}
        kvs = param_str.split('&')
        for kv in kvs:
            [k,v]= kv.split('=', 1)
            self.params[k] = v


def msg_encode(msg):
    res  = '%s\r\n'%msg.meta
    body_len = 0
    if msg.body is not None:
        body_len = len(msg.body) 
        
    for k,v in msg.head.iteritems():
        res += '%s: %s\r\n'%(k,v)
    len_key = 'content-length'
    if len_key not in msg.head:
        res += '%s: %s\r\n'%(len_key, body_len)
    
    res += '\r\n'
    if msg.body is not None:
        res += msg.body    
    return res


def find_header_end(buf, start=0):
    i = start
    end = len(buf)
    while i+3<end:
        if buf[i]=='\r' and buf[i+1]=='\n' and buf[i+2]=='\r' and buf[i+3]=='\n':
            return i+3
        i += 1
    return -1

def decode_headers(buf):
    msg = Message()
    buf = str(buf)
    lines = buf.splitlines() 
    msg.meta = Meta(lines[0]) 
    
    for i in range(1,len(lines)):
        line = lines[i] 
        if len(line) == 0: continue
        try:
            p = line.index(':') 
            key = str(line[0:p]).strip()
            val = str(line[p+1:]).strip()
            msg.head[key] = val
        except Exception, e:
            logging.error(e)
            
    params = msg.meta.params
    if params:
        for k,v in params.iteritems():
            if k not in msg.head:
                msg.head[k] = v
            
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
    body_len = msg.get_head('content-length') 
    if body_len is None: 
        return (msg, p)
    body_len = int(body_len)
    if len(buf)-p < body_len:
        return (None, start)
    
    msg.body = buf[p: p+body_len]
    return (msg,p+body_len) 


class MqMode:
    MQ     = 1<<0
    PubSub = 1<<1   
    Memory = 1<<2
  
        
class Message: 
    REMOTE_ADDR= "remote-addr"
    ENCODING   = "encoding"
    
    CMD     = "cmd"
    SUB_CMD = "sub_cmd"
    BROKER  = "broker"
    TOPIC   = "topic"  
    MQ      = "mq" 
    ID      = "id"
    RAWID   = "rawid"
    ACK     = "ack"
    SENDER  = "sender"
    RECVER  = "recver"
    REPLY_CODE = "reply_code"
     
        
    def __init__(self):
        self.meta = Meta()
        self.head = {}
        self.body = None
       
    def __str__(self):
        return msg_encode(self)
    
    def get_head(self, key, default_value=None):
        value = self.head.get(key)
        if value is None:
            value = default_value
        return value
    
    def set_head(self, key, value):
        self.head[key] = value
        
    def remove_head(self, key):
        if key in self.head:
            del self.head[key]
    
    def set_body(self, body):
        self.body = body
        self.head['content-length'] = '%d'%len(self.body)
        
    def set_json_body(self, body):
        self.head['content-type']= 'application/json'
        self.set_body(body)

        
    #################################################################
    def get_cmd(self): 
        return self.get_head(self.CMD)
    def set_cmd(self, value):
        self.set_head(self.CMD, value)
    def get_sub_cmd(self): 
        return self.get_head(self.SUB_CMD)
    def set_sub_cmd(self, value):
        self.set_head(self.SUB_CMD, value)
    def get_id(self): 
        return self.get_head(self.ID)
    def set_id(self, value):
        self.set_head(self.ID, value)
    def get_rawid(self): 
        return self.get_head(self.RAWID)
    def set_rawid(self, value):
        self.set_head(self.RAWID, value)
    def get_mq(self): 
        return self.get_head(self.MQ)
    def set_mq(self, value):
        self.set_head(self.MQ, value)    
    def get_topic(self): 
        return self.get_head(self.TOPIC)
    def set_topic(self, value):
        self.set_head(self.TOPIC, value)   
    def get_encoding(self): 
        return self.get_head(self.ENCODING)
    def set_encoding(self, value):
        self.set_head(self.ENCODING, value) 
    def get_reply_code(self): 
        return self.get_head(self.REPLY_CODE)
    def set_reply_code(self, value):
        self.set_head(self.REPLY_CODE, value)   
    
    def get_sender(self): 
        return self.get_head(self.SENDER)
    def set_sender(self, value):
        self.set_head(self.SENDER, value)      
    def get_recver(self): 
        return self.get_head(self.RECVER)
    def set_recver(self, value):
        self.set_head(self.RECVER, value)  
           
    def is_ack(self):
        ack = self.get_head(self.ACK)
        if ack is None: return True
        return ack == '1'
    
    def set_ack(self, value):
        if value==True:
            value = '1'
        else:
            value = '0'
        self.set_head(self.ACK, value)
        
        
    def get_status(self): 
        return self.meta.status
    def set_status(self, value): 
        if value:
            self.meta.status = str(value)
        else:
            self.meta.status = None  
    
    def is_status200(self):
        return '200' == self.meta.status 
    def is_status404(self):
        return '404' == self.meta.status 
    def is_status500(self):
        return '500' == self.meta.status
   
class Proto:
    Produce      = "produce"    #生产消息
    Consume      = "consume"    #消费消息    
    Route        = "route"      #请求等待应答消息  
    CreateMQ     = "create_mq"  
    
    Heartbeat    = "heartbeat"; #心跳消息   
    Admin        = "admin"      #管理类消息
     


#线程不安全
class MessageClient(object):
    log = logging.getLogger(__name__)
    def __init__(self, host='localhost', port=15555): 
        self.pid = os.getpid()
        self.host = host
        self.port = port;
        
        self.read_buf = ''  
        self.sock = None  
        self.id = uuid.uuid4()
        self.auto_reconnect = True
        self.reconnect_interval = 3 #3 seconds
        self.msg_id_match = ''
        self.result_table = {} 
        self.msg_cb = None
    
    def close(self):
        if self.sock is not None:
            self.sock.close()
            self.sock = None
            self.read_buf = ''
            
    def connect_if_need(self):
        if self.sock is None: 
            self.read_buf = ''
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.connect( (self.host, self.port) )
            self.log.info('Connected to (%s:%s)'%(self.host, self.port))

    def reconnect(self):
        if self.sock is not None:
            self.sock.close()
            self.sock = None
            self.read_buf = ''
        while self.sock is None:
            try:
                self.read_buf = ''
                self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                self.log.debug('Trying reconnect to (%s:%s)'%(self.host, self.port))
                self.sock.connect( (self.host, self.port) )  
                self.log.debug('Connected to (%s:%s)'%(self.host, self.port))
            except socket.error, e:
                self.sock = None
                if self.auto_reconnect:
                    time.sleep(self.reconnect_interval)
                else:
                    raise e
    def mark_msg(self, msg):
        if msg.get_id(): #msg got id, do nothing
            return
        self.msg_id_match = str(uuid.uuid4())
        msg.set_id(self.msg_id_match)
    
    def invoke(self, msg, timeout=10):  
        self.send(msg, timeout)
        return self.recv(timeout)
    
    def send(self, msg, timeout=10):
        self.connect_if_need() 
        self.mark_msg(msg)
        self.log.debug('Request: %s'%msg)
        self.sock.sendall(str(msg))  
        
    def recv(self, timeout=10):
        if self.msg_id_match in self.result_table:
            return self.result_table[self.msg_id_match]
        self.connect_if_need()
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
                if self.msg_cb: #using msg callback
                    self.msg_cb(msg)
                    continue
                
                if self.msg_id_match and msg.get_id() != self.msg_id_match:
                    self.result_table[msg.get_id()] = msg
                    continue 
                self.log.debug('Result: %s'%msg) 
                return msg 



class ClientHint:
    def __init__(self):
        self.mq = None
        self.broker = None
        self.requestIp = None
        
class ClientPool:
    log = logging.getLogger(__name__)
    def __init__(self, host='localhost', port=15555,  maxsize=50, timeout=10):   
        self.client_class = MessageClient
        self.host = host
        self.port = port
        self.maxsize = maxsize
        self.timeout = timeout
        
        self.reset()
    
    def make_client(self):
        client = MessageClient(host=self.host, port=self.port) 
        self.clients.append(client)
        self.log.debug('New client created %s', client)
        return client
    
    def _check_pid(self):
        if self.pid != os.getpid():
            with self._check_lock:
                if self.pid == os.getpid(): 
                    return
                self.log.debug('new process, pid changed')
                self.destroy()
                self.reset()
                    
    def reset(self):
        self.pid = os.getpid() 
        self._check_lock = threading.Lock()
        
        self.client_pool = Queue.LifoQueue(self.maxsize)
        while True:
            try:
                self.client_pool.put_nowait(None)
            except Queue.Full:
                break 
        self.clients = []
        
    
    def borrow_client(self, hint=None): #mq ignore
        self._check_pid()
        client = None
        try:
            client = self.client_pool.get(block=True, timeout=self.timeout)
        except Queue.Empty: 
            raise Exception('No client available')
        if client is None:
            client = self.make_client()
        return client 
    
    def return_client(self, client):
        self._check_pid()
        if client.pid != self.pid:
            return 
        if not isinstance(client, (tuple, list)):
            client = [client]
        for c in client:
            try:
                self.client_pool.put_nowait(c)
            except Queue.Full: 
                pass
            
    def destroy(self):
        for client in self.clients:
            client.close()
            
class Broker(object):
    def get_client(self, client_hint=None):
        raise NotImplementedError("unimplemented")
    def close_client(self, client):
        raise NotImplementedError("unimplemented")
    def inovke(self, msg):
        raise NotImplementedError("unimplemented")
    def destroy(self):
        raise NotImplementedError("unimplemented")

class SingleBroker(Broker): 
    def __init__(self, host='localhost', port=15555, maxsize=128):
        self.host = host
        self.port = port
        self.pool = ClientPool(host=host, port=port, maxsize=maxsize)

    def get_client(self, client_hint=None):
        return MessageClient(self.host, self.port)
    
    def close_client(self, client):
        if client:
            client.close()
            
    def invoke(self, msg, timeout=10):
        client = self.pool.borrow_client() 
        try:
            return client.invoke(msg, timeout)
            
        finally: 
            self.pool.return_client(client)
            
    def destroy(self):
        self.pool.destroy()


class Producer:
    def __init__(self, broker=None, mq = None):
        self.broker = broker
        self.mq = mq 
    
    def send(self, msg):
        msg.set_cmd(Proto.Produce)
        msg.set_mq(self.mq) 
        return self.broker.invoke(msg)


class Consumer:
    log = logging.getLogger(__name__)
    def __init__(self, broker, mq = None, mode=MqMode.MQ, topic=None):
        self.broker = broker
        self.mq = mq 
        self.mode = mode 
        self.topic = topic
        
        self.client = None
    
    def myClientHint(self):
        return None
    
    def createMQ(self): 
        msg = Message()
        msg.set_cmd(Proto.CreateMQ)
        msg.set_head("mq_name", self.mq)
        msg.set_head("mq_mode", self.mode)
      
        res = self.client.invoke(msg, 10)
        return res.is_status200()
    
    def recv(self, timeout=10):  
        if self.client is None:
            hint = self.myClientHint()
            self.client = self.broker.get_client(hint)
            
        msg = Message() 
        msg.set_cmd(Proto.Consume)
        msg.set_mq(self.mq) 
        if self.mode & MqMode.PubSub:
            if self.topic:
                msg.set_topic(self.topic)
        try:
            res = self.client.invoke(msg, timeout)
            if res.is_status404():
                if not self.createMQ():
                    raise Exception('MQ(%s) creation failed')
                return self.recv(timeout) 
            res.set_id(res.get_rawid())
            res.remove_head(Message.RAWID)
            res.remove_head(Message)
            return res
        except socket.timeout: #等待消息超时
            return None
        except socket.error, e: #网络错误
            self.log.debug(e)
            hint = self.myClientHint()
            self.broker.close_client(self.client)
            self.client = self.broker.get_client(hint)
            return self.recv(timeout) 
            

    def route(self, msg, timeout=10):
        status = msg.get_status() 
        if status:
            msg.set_reply_code(status);
            msg.set_status(None) 
            
        msg.set_cmd(Proto.Route);  
        msg.set_ack(False)
        self.client.send(msg)

class Caller:     
    log = logging.getLogger(__name__) 
    def __init__(self, broker = None, mq = None):
        self.broker = broker
        self.mq = mq
      
    
    def invoke(self, msg, timeout=10): 
        msg.set_cmd(Proto.Produce)
        msg.set_mq(self.mq) 
        msg.set_ack(False)
        self.log.debug('Request: %s'%msg) 
        res = self.broker.invoke(msg, timeout)
        self.log.debug('Result: %s'%res)
        return res        

class ServiceHandler(object):  
    def __call__(self, req):
        return self.handle_request(req)   
        
    def handle_request(self, msg):   
        raise Exception('unimplemented') 


class ServiceConfig:
    def __init__(self):
        self.service_andler = None;
        self.broker = None;  
        self.service_name = None; 
        self.thread_count = 1;
        self.consume_timeout = 10; #seconds
        
class WorkerThread(threading.Thread):
    def __init__(self, config):
        threading.Thread.__init__(self)   
        self.handler = config.service_andler
        if not isinstance(self.handler, ServiceHandler):
            raise Exception('handler not support')
        
        self.mq = config.service_name 
        self.broker = config.broker 
        self.consume_timeout = config.consume_timeout
        
        
    def run(self):   
        consumer = Consumer(broker=self.broker, mq=self.mq)
        
        while True:
            msg = consumer.recv(self.consume_timeout)
            if msg is None: continue
            sender = msg.get_sender()
            msgid = msg.get_id()
            
            res = self.handler.handle_request(msg)
            if res is None: continue
            res.set_id(msgid)
            res.set_recver(sender)
            
            consumer.route(res, self.consume_timeout)
        
    
class Service(threading.Thread):   
    def __init__(self, config):
        threading.Thread.__init__(self)   
        self.config = config
        self.thread_count = config.thread_count
        
    def run(self):  
        workers = []
        for i in range(self.thread_count):
            workers.append(WorkerThread(self.config))
        for w in workers:
            w.start()
        for w in workers:
            w.join()   
            

class MyServiceHandler(ServiceHandler):
    def handle_request(self, msg): 
        print msg
        res = Message()
        res.set_status('200')
        res.set_body('hello server@%s'%time.time())
        return res

        
def Remote( _id = None ):
    def func(fn):
        fn.remote_id = _id or fn.__name__ 
        return fn
    return func 

class Rpc(Caller):
    log = logging.getLogger(__name__)  
    def __init__(self, broker=None, mq = None, module='', mehtod=None, 
                 timeout=10, encoding='utf8'):
        Caller.__init__(self, broker=broker, mq = mq)  
        self.timeout = timeout
        self.method = mehtod
        self.module = module
        self.encoding = encoding
    
    def __getattr__(self, name):
        rpc = Rpc(broker=self.broker, 
                  mq=self.mq, 
                  module=self.module,
                  timeout=self.timeout,
                  encoding=self.encoding) 
        
        rpc.method = name; 
        return rpc
    
    def _error_msg(self, msg_req):
        return '=========RPC Context=========\nMQ(%s)-Module(%s)-Method(%s)\n=========Message Dump========\n%s'%(self.mq, self.module, self.method, msg_req)
    
    def invoke(self, args): 
        req = {'module': self.module, 'method': self.method, 'params': args}
        msg_req = Message() 
        msg_req.set_json_body(json.dumps(req, encoding=self.encoding)) 
        
        try:
            msg_res = Caller.invoke(self, msg_req, self.timeout)
        except socket.timeout, e:
            error_msg = 'Request Timeout\n%s'%(self._error_msg(msg_req))
            raise Exception(error_msg) 
        except socket.error, e:
            error_msg = '%s\n%s'%(e, self._error_msg(msg_req))
            raise Exception(error_msg) 
        
        if msg_res is None:
            error_msg = self._error_msg(msg_req)
            raise Exception(error_msg)
        
        if msg_res.is_status404():
            msg_res_body = msg_res.body
            error_msg = '%s\n%s'%(msg_res_body, self._error_msg(msg_req))
            raise Exception(error_msg)
        
        res = json.loads(msg_res.body, encoding=msg_res.get_encoding()) 
        
        if not msg_res.is_status200():
            error_text = 'unknown error' 
            if 'stackTrace' in res: error_text = res['stackTrace']
            elif 'error' in res: error_text = res['error'] 
            error_msg = '%s\n%s'%(error_text, self._error_msg(msg_req))
            raise Exception(error_msg)
        
        if 'result' in res:
            return res['result']
        
        error_text = 'bad json result format'
        error_msg = '%s\n%s'%(error_text, self._error_msg(msg_req))
        raise Exception(error_msg) 
    
    def __call__(self, *args): 
        return self.invoke(args)  

class RpcServiceHandler(ServiceHandler):
    def __init__(self): 
        self.methods = {} 
    
    def add_module(self, module, service):
        methods = inspect.getmembers(service, predicate=inspect.ismethod)
        for method in methods:
            if hasattr(method[1], 'remote_id'):
                remote_id = getattr(method[1], 'remote_id')
                key = '%s:%s'%(module,remote_id)
                if key in self.methods:
                    print '%s duplicated'%key
                self.methods[key] = method[1]
    
    def handle_request(self, msg):    
        try:
            encoding = msg.get_encoding()
            if encoding is None:
                encoding = 'utf8'
            
            return self.handle_json_request(msg.body, encoding)
        except Exception, error:
            msg = Message()
            msg.set_status('500')
            msg.set_json_body(json.dumps({'error': str(error), 'stack_trace': str(error)}, encoding=encoding))
            return  msg
    
    def handle_json_request(self, json_str, encoding='utf-8'):
        error = None
        result = None 
        status = '400'
        try:
            req = json.loads(json_str, encoding=encoding) 
        except Exception, e: 
            error = Exception('json format error: %s'%str(e))
            
        if error is None:
            try: 
                module = req['module']
                method = req['method']
                params = req['params']
            except:
                error = Exception('parameter error: %s'%json_str)
        
        if error is None:
            key = '%s:%s'%(module,method)
            if key not in self.methods:
                error = Exception('%s method not found'%key) 
            else:
                method = self.methods[key]
        
        if error is None:
            try:
                result = method(*params)
            except Exception, e:
                error = e
        
        #return result
        try:
            if error is not None: 
                data = json.dumps({'error': str(error), 'stack_trace': str(error)}, encoding=encoding)
            else:
                status = '200'
                data = json.dumps({'result': result}, encoding=encoding)
        except:
            status = '500' 
            data = json.dumps({'error': error })
        
        msg = Message()
        msg.set_status(status)
        msg.set_json_body(data)
        
        return msg     

######################KCXP##############################    


######################TRADE##############################   



__all__ = [
    Proto,MqMode, Message,MessageClient,SingleBroker,
    Producer, Consumer, Caller,
    ServiceConfig, Service,
    Remote, Rpc, 
    ServiceHandler, RpcServiceHandler
]    