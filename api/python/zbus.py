#encoding=utf8
import socket, time, threading, thread, json, inspect, uuid, os
try:
    import Queue
except:
    import queue as Queue
    
import logging, logging.config 
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



class Proto:
    Produce      = "produce"    #生产消息
    Consume      = "consume"    #消费消息    
    Request      = "request"    #请求等待应答消息  
    Heartbeat    = "heartbeat"; #心跳消息   
    Admin        = "admin"      #管理类消息
    CreateMQ     = "create_mq"   
    #TrackServer通讯
    TrackReport  = "track_report" 
    TrackSub     = "track_sub" 
    TrackPub     = "track_pub"

    @staticmethod
    def build_admin_message(admin_token, cmd, params):
        msg = Message()
        msg.set_command(Proto.Admin)
        msg.set_token(admin_token)
        msg.set_head('cmd', cmd)
        if params is not None:
            for k,v in params.iteritems():
                msg.set_head(k,v)
                
        return msg

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
        self.command = None
        self.params = None
        
        if meta is None or meta == '':
            return
        blocks = meta.split(None, 2)
        method = blocks[0]
        if not method in Meta.http_method: #Response
            self.status = blocks[1]
            return
        
        self.method = method
        self._decode_command(blocks[1])
        
    
    def __str__(self):   
        if self.status is not None:
            desc = Meta.http_status.get(self.status)
            if desc is None: desc = "Unknown Status"
            return "HTTP/1.1 %s %s"%(self.status, desc)
        if self.command is not None:
            cmdStr = self._encode_command()
            return "%s /%s HTTP/1.1"%(self.method, cmdStr)
        return ""
    
    
    def get_command(self):
        return self.command
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
    
    def set_param(self, key, value):
        if self.params is None:
            self.params = {}
        self.params[key] = value
        
   
    def _encode_command(self):
        res = ""
        if self.command is not None:
            res += self.command
        if self.params is None:
            return res
        
        if len(self.params) > 0:
            res += "?"
        for k,v in self.params.iteritems():
            res += "%s=%s&"%(k,v)
        if res.endswith('&'):
            res = res[0: len(res)-1]
        return res
    
    def _decode_command(self, cmd_str):
        cmd_str = str(cmd_str)
        idx = cmd_str.find('?')
        if idx < 0 :
            self.command = cmd_str
        else:
            self.command = cmd_str[0:idx]
        if self.command[0] == '/':
            self.command = self.command[1:]
        
        if idx < 0: return
        
        param_str = cmd_str[idx+1:]
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
        except:
            pass
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


class MessageMode:
    MQ     = 1<<0
    PubSub = 1<<1   
    Temp   = 1<<2
  
        
class Message: 
    HEADER_CLIENT     = "remote-addr"
    HEADER_ENCODING   = "content-encoding"
    
    HEADER_BROKER     = "broker"
    HEADER_TOPIC      = "topic" 
    HEADER_MQ_REPLY   = "mq-reply"
    HEADER_MQ         = "mq"
    HEADER_TOKEN      = "token"
    HEADER_MSGID      = "msgid"
    HEADER_MSGID_RAW  = "msgid-raw"
    HEADER_ACK        = "ack"
    
    HEADER_REPLY_CODE = "reply-code"
        
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
    
    def get_head_or_param(self, key, default_value=None):
        value = self.get_head(key)
        if value is None:
            value = self.meta.get_param(key, default_value)
        return value
    
    def set_body(self, body):
        self.body = body
        self.head['content-length'] = '%d'%len(self.body)
        
    def set_json_body(self, body):
        self.head['content-type']= 'application/json'
        self.set_body(body)

        
    #################################################################
    def get_mq_reply(self):
        return self.get_head_or_param(self.HEADER_MQ_REPLY)
    def set_mq_reply(self, value):
        self.set_head(self.HEADER_MQ_REPLY, value)
    def get_msgid(self): 
        return self.get_head_or_param(self.HEADER_MSGID)
    def set_msgid(self, value):
        self.set_head(self.HEADER_MSGID, value)
    def get_msgid_raw(self): 
        return self.get_head_or_param(self.HEADER_MSGID_RAW)
    def set_msgid_raw(self, value):
        self.set_head(self.HEADER_MSGID_RAW, value)
    def get_mq(self): 
        return self.get_head_or_param(self.HEADER_MQ)
    def set_mq(self, value):
        self.set_head(self.HEADER_MQ, value)    
    def get_token(self): 
        return self.get_head_or_param(self.HEADER_TOKEN)
    def set_token(self, value):
        self.set_head(self.HEADER_TOKEN, value)  
    def get_topic(self): 
        return self.get_head_or_param(self.HEADER_TOPIC)
    def set_topic(self, value):
        self.set_head(self.HEADER_TOPIC, value)   
    def get_encoding(self): 
        return self.get_head_or_param(self.HEADER_ENCODING)
    def set_encoding(self, value):
        self.set_head(self.HEADER_ENCODING, value)  
    def is_ack(self):
        ack = self.get_head_or_param(self.HEADER_ACK)
        if ack is None: return True
        return ack == '1'
    
    def set_ack(self, value):
        if value==True:
            value = '1'
        else:
            value = '0'
        self.set_head(self.HEADER_ACK, value)
        
    def get_command(self): 
        return self.meta.command
    def set_command(self, value):
        self.meta.status = None
        self.meta.command = value
    def get_status(self): 
        return self.meta.status
    def set_status(self, value):
        self.meta.command = None
        self.meta.status = str(value)  
    
    def is_status200(self):
        return '200' == self.meta.status 
    def is_status404(self):
        return '404' == self.meta.status 
    def is_status500(self):
        return '500' == self.meta.status
   

#线程不安全
class RemotingClient(object):
    log = logging.getLogger(__name__)
    def __init__(self, **kwargs): 
        self.pid = os.getpid()
        broker = kwargs.get('broker', '127.0.0.1:15555')
        blocks = broker.split(':')
        self.host = blocks[0];
        self.port = 15555;
        if len(blocks)>1:
            self.port = int(blocks[1])
            
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
        if msg.get_msgid(): #msg got id, do nothing
            return
        self.msg_id_match = str(uuid.uuid4())
        msg.set_msgid(self.msg_id_match)
    
    def invoke(self, msg, timeout=10): 
        self.send(msg, timeout)
        return self.recv(timeout)
    
    def send(self, msg, timeout=10):
        self.connect_if_need() 
        self.mark_msg(msg)
        self.log.debug('Request: %s'%msg)
        self.sock.send(str(msg))  
        
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
                
                if self.msg_id_match and msg.get_msgid() != self.msg_id_match:
                    self.result_table[msg.get_msgid()] = msg
                    continue 
                self.log.debug('Result: %s'%msg) 
                return msg 

class IClientPool(object):
    def borrow_client(self, mq):
        raise NotImplementedError("unimplemented")
    def borrow_each_client(self, mq):
        raise NotImplementedError("unimplemented")
    def return_client(self, client_or_clients):
        raise NotImplementedError("unimplemented")
    def destroy(self):
        raise NotImplementedError("unimplemented")

class IClientBuilder(object):
    def create_client_for_mq(self, mq):
        raise NotImplementedError("unimplemented")
    def create_client_for_broker(self, broker):
        raise NotImplementedError("unimplemented")
    

class ClientPool(IClientPool):
    log = logging.getLogger(__name__)
    def __init__(self, max_clients=50, timeout=10, **client_kwargs):   
        self.client_class = RemotingClient
        self.client_kwargs = client_kwargs
        self.max_clients = max_clients
        self.timeout = timeout
        
        self.reset()
    
    def _check_pid(self):
        if self.pid != os.getpid():
            with self._check_lock:
                if self.pid == os.getpid(): 
                    return
                self.log.debug('new process, pid changed')
                self.disconnect()
                self.reset()
                    
    def reset(self):
        self.pid = os.getpid() 
        self._check_lock = threading.Lock()
        
        self.client_pool = Queue.LifoQueue(self.max_clients)
        while True:
            try:
                self.client_pool.put_nowait(None)
            except Queue.Full:
                break 
        self.clients = []
        
    def make_client(self):
        client = RemotingClient(**self.client_kwargs) 
        self.clients.append(client)
        self.log.debug('New client created %s', client)
        return client
    
    def borrow_client(self, mq): #mq ignore
        self._check_pid()
        client = None
        try:
            client = self.client_pool.get(block=True, timeout=self.timeout)
        except Queue.Empty: 
            raise Exception('No client available')
        if client is None:
            client = self.make_client()
        return client
     
    def borrow_each_client(self, mq):
        return [self.borrow_client(mq)]
    
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


class ClientAgent(IClientPool, IClientBuilder):
    log = logging.getLogger(__name__)
    def __init__(self, **kwargs):
        self.seed_broker = kwargs.get('seed_broker', '127.0.0.1:15555') #could be none
        self.track_server = kwargs.get('track_server', '127.0.0.1:16666;127.0.0.1:16667')
        self.track_table = {}
        self.track_clients = []
        self.query_interval = int(kwargs.get('query_interval', 3)) #3 seconds
        
        self.pool_max_clients = int(kwargs.get('pool_max_clients', 30))
        self.pool_timeout = int(kwargs.get('pool_timeout', 10))
        self.pool_table = {} #broker=>ClientPool
        
        self.ready_event = threading.Event() 
        self.ready = False
        self.ready_timeout = 3 #3 seconds 
        self.connect()
        
    
    def close(self):   
        for client in self.track_clients:
            client.close()
    
    def _get_mqinfo_list(self, mq): 
        mq_table = self.track_table.get('mqTable', {})
        return mq_table.get(mq, [])       
    def _get_broker_by_mq(self, mq): 
        mqinfo_list = self._get_mqinfo_list(mq)
        if len(mqinfo_list)>0:
            return mqinfo_list[0]['broker'] 
        broker_list = self.track_table.get('brokerList', [])
        if len(broker_list)>0:
            return broker_list[0]
        return self.seed_broker
    
    #####################IClientBuilder IMPL######################
    def create_client_for_broker(self, broker): 
        broker_list = self.track_table.get('brokerList', [])
        if broker not in broker_list:
            raise Exception('Broker(%s) not found in track table'%broker)
        return RemotingClient(broker=broker)
    
    def create_client_for_mq(self, mq):
        broker = self._get_broker_by_mq(mq)
        return RemotingClient(broker=broker) 
    
    #####################IClientPool IMPL######################
    def _borrow_client_by_broker(self, broker):
        pool = self.pool_table.get(broker)
        if pool is None:
            raise Exception('Pool(%s) not found'%broker)
        client = pool.borrow_client(None)
        client.broker = broker #set broker
        return client
    
    def borrow_client(self, mq):
        broker = self._get_broker_by_mq(mq)
        return self._borrow_client_by_broker(broker)
    
    def borrow_each_client(self, mq): 
        clients = []
        broker_list = []
        mqinfo_list = self._get_mqinfo_list(mq)
        for mqinfo in mqinfo_list:
            broker_list.append(mqinfo['broker'])
        if len(broker_list) == 0:
            broker_list.append(self.seed_broker)
        
        for broker in broker_list: 
            client = self._borrow_client_by_broker(broker)
            clients.append(client)
        return clients
    
    def _return_client(self, client):
        broker = client.broker
        pool = self.pool_table.get(broker)
        if pool is None:
            raise Exception('Pool(%s) not found'%broker)
        return pool.return_client(client)
    
    def return_client(self, client):
        if client is None: return
        
        if not isinstance(client, (tuple, list)):
            client = [client]
        for c in client:
            try:
                self._return_client(c)
            except:
                pass
    
    
    def connect(self):     
        def on_track_updated(msg):  
            self.log.debug(msg)
            encoding = msg.get_encoding()
            if encoding is None: encoding = 'utf8'
            try:
                self.track_table = json.loads(msg.body, encoding=encoding) 
            except Exception, e:
                self.log.error(e) 
                return
            
            broker_list = self.track_table.get('brokerList', [])
            for broker in broker_list:
                if broker in self.pool_table:
                    continue
                self.log.info('New ClientPool(%s)'%broker)
                pool = ClientPool(self.pool_max_clients,
                                  self.pool_timeout, broker=broker)
                self.pool_table[broker] = pool
            
            if not self.ready:
                self.log.debug('track table ready')
                self.ready = True
                self.ready_event.set() 
            
        
        def sub_track_table_with_client(client):
            msg = Message()
            msg.set_command(Proto.TrackSub) 
            while True:
                try:
                    client.msg_cb = on_track_updated
                    client.invoke(msg, self.query_interval)
                except socket.timeout: 
                    continue
                except socket.error:
                    client.reconnect()
                    return sub_track_table_with_client(client)
                
        
        def sub_track_table(ts_address):
            client = RemotingClient(broker=ts_address) 
            sub_track_table_with_client(client)
        
        ts_array = self.track_server.split(';')    
        for ts_address in ts_array:
            thread.start_new_thread(sub_track_table, (ts_address,))
        
        self.ready_event.wait(self.ready_timeout)
        
class Consumer:
    log = logging.getLogger(__name__)
    def __init__(self, **kwargs): 
        self.mq = kwargs.get('mq', None)
        if self.mq is None:
            raise Exception('mq required')
        self.client = kwargs.get('client', None)
        self.client_builder = kwargs.get('client_builder', None)
        if self.client_builder:
            assert isinstance(self.client_builder, IClientBuilder)
        if self.client is None and self.client_builder is None:
            raise Exception('ClientBuilder/Client both missing')
        
        self.auto_register = kwargs.get('auto_register', True)
        self.access_token = kwargs.get('access_token', '')
        self.register_token = kwargs.get('register_token', '')
        self.message_mode = kwargs.get('mode', MessageMode.MQ)
        self.use_client_builder = False
        self.topic = None
        
        if self.client is None: 
            self.client = self.client_builder.create_client_for_mq(self.mq)
            self.use_client_builder = True

    
    def handle_fail_over(self):
        if self.use_client_builder:
            assert self.client_builder
            self.client.close()
            self.log.debug('Trying create new client via ClientBuilder')
            self.client = self.client_builder.create_client_for_mq(self.mq)
        else: 
            self.client.reconnect()    
    
    def register(self, timeout=10):
        params = {'mq_name': self.mq, 
                  'access_token': self.access_token,
                  'mq_mode': self.message_mode}
        msg = Proto.build_admin_message(self.register_token, Proto.CreateMQ, params)
        res = self.client.invoke(msg, timeout)
        return res.is_status200()
    
    
    def recv(self, timeout=10):  
        msg = Message() 
        msg.set_command(Proto.Consume)
        msg.set_mq(self.mq)
        msg.set_token(self.access_token)
        if self.message_mode & MessageMode.PubSub:
            if self.topic:
                msg.set_topic(self.topic)
        try:
            res = self.client.invoke(msg, timeout)
            if res.is_status404() and self.auto_register:
                if not self.register(timeout):
                    raise Exception('register error')
                return self.recv(timeout) 
            return res
        except socket.timeout: #等待消息超时
            return None
        except socket.error, e: #网络错误
            self.log.debug(e)
            self.handle_fail_over()
            return self.recv(timeout) 
            

    def reply(self, msg, timeout=10):
        msg.set_head(Message.HEADER_REPLY_CODE, msg.get_status());
        msg.set_command(Proto.Produce);  
        msg.set_ack(False)
        self.client.send(msg)
        


def invoke_sync(pool, client, msg, timeout=10):
    if client is not None: #优先使用Client
        return client.invoke(msg, timeout) 
    assert pool is not None
    mq = msg.get_mq()
    try:
        client = pool.borrow_client(mq)
        return client.invoke(msg, timeout)
    finally:
        pool.return_client(client)

def invoke_sync_all(pool, client, msg, timeout=10):
    if client is not None: #优先使用Client
        return client.invoke(msg, timeout) 
    assert pool is not None
    mq = msg.get_mq() 
    try:
        clients = pool.borrow_each_client(mq)
        results = []
        for client in clients:
            try:
                res = client.invoke(msg, timeout)
                results.append(res)
            except Exception, e:
                results.append(e)
        return results
    finally:
        pool.return_client(clients)        

class Producer:
    def __init__(self, **kwargs): 
        self.mq = kwargs.get('mq', None)
        if self.mq is None:
            raise Exception('mq required')
        self.client = kwargs.get('client', None)
        self.client_pool = kwargs.get('client_pool', None)
        if self.client is None and self.client_pool is None:
            raise Exception('ClientPool/Client both missing')
        self.token = kwargs.get('token', '') 
        self.message_mode = kwargs.get('mode', MessageMode.MQ)

    def send(self, msg, timeout=10):   
        msg.set_command(Proto.Produce)
        msg.set_mq(self.mq)
        msg.set_token(self.token)
        
        if self.message_mode & MessageMode.PubSub:
            return invoke_sync_all(self.client_pool, self.client, msg, timeout)
        else: #default to MQ mode
            return invoke_sync(self.client_pool, self.client, msg, timeout)

############################RPC########################
class ServiceHandler(object):  
    def __call__(self, req):
        return self.handle_request(req)   
        
    def handle_request(self, msg):   
        raise Exception('unimplemented')  
         
class Rpc(object):     
    log = logging.getLogger(__name__) 
     
    def __init__(self, **kwargs):  
        self.mq = kwargs.get('mq', None)
        if self.mq is None:
            raise Exception('mq required')
        self.client = kwargs.get('client', None)
        self.client_pool = kwargs.get('client_pool', None)
        if self.client is None and self.client_pool is None:
            raise Exception('ClientPool/Client both missing')
        if self.client and self.client_pool:
            self.log.warn('ClientPool/Client both set, but default to client')
        self.token = kwargs.get('token', '') 
        self.encoding = kwargs.get('encoding', 'utf8') 
    
    def invoke(self, msg, timeout=10): 
        msg.set_command(Proto.Request)
        msg.set_mq(self.mq)
        msg.set_token(self.token)
        msg.set_encoding(self.encoding)
        self.log.debug('Request: %s'%msg)
        res = invoke_sync(self.client_pool, self.client, msg, timeout)
        self.log.debug('Result: %s'%res)
        return res
    

class RpcServiceConfig:
    def __init__(self):
        self.service_andler = None;
        self.broker = None; 
        self.client_builder = None;
        self.service_name = None;
        self.register_token = "";
        self.access_token = ""; 
        self.thread_count = 1;
        self.consume_timeout = 10; #seconds
        
class WorkerThread(threading.Thread):
    def __init__(self, config):
        threading.Thread.__init__(self)   
        self.handler = config.service_andler
        if not isinstance(self.handler, ServiceHandler):
            raise Exception('handler not support')
        
        self.mq = config.service_name
        self.register_token = config.register_token
        self.access_token = config.access_token
        self.broker = config.broker 
        self.consume_timeout = config.consume_timeout
        
        self.client_builder = config.client_builder
        self.client = None
        if self.broker is not None:
            self.client = RemotingClient(broker=self.broker) 
        
        
    def run(self):   
        consumer = Consumer(client=self.client, 
                            client_builder=self.client_builder, 
                            mq=self.mq)
        consumer.register_token = self.register_token
        consumer.access_token = self.access_token
        
        while True:
            msg = consumer.recv(self.consume_timeout)
            if msg is None: continue
            mq_reply = msg.get_mq_reply()
            msgid = msg.get_msgid_raw()
            
            res = self.handler.handle_request(msg)
            if res is None: continue
            res.set_msgid(msgid)
            res.set_mq(mq_reply)
            
            consumer.reply(res, self.consume_timeout)
        
    
class RpcService(threading.Thread):   
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
            

################################JSON-RPC##################################
def Remote( _id = None ):
    def func(fn):
        fn.remote_id = _id or fn.__name__ 
        return fn
    return func 

class JsonRpc(Rpc):
    def __init__(self, **kwargs):
        Rpc.__init__(self, **kwargs) 
        self.kwargs = kwargs
        self.timeout = int(kwargs.get('timeout', 10))
        self.method = kwargs.get('method', None)
        self.module = kwargs.get('module', '')
    
    def __getattr__(self, name):
        rpc = JsonRpc(**self.kwargs) 
        rpc.method = name; 
        return rpc
    
    def invoke(self, args): 
        req = {'module': self.module, 'method': self.method, 'params': args}
        msg = Message() 
        msg.set_json_body(json.dumps(req, encoding=self.encoding)) 
        return Rpc.invoke(self, msg, self.timeout)
    
    def __call__(self, *args): 
        res = self.invoke(args)  
        obj = json.loads(res.body, encoding=res.get_encoding()) 
        if not res.is_status200():
            msg = 'unknown error'
            if 'stack_trace' in obj: msg = obj['stack_trace']
            raise Exception(msg)
        if 'result' in obj:
            return obj['result']
        raise Exception('bad json result format')


class JsonServiceHandler(ServiceHandler):
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
    
__all__ = [
    Proto,MessageMode, Message,RemotingClient,ClientPool,ClientAgent,
    Consumer,Producer,ServiceHandler,
    Rpc,RpcServiceConfig,RpcService,
    Remote, JsonRpc, JsonServiceHandler
]    
