#encoding=utf8
import sys 
sys.path.append('../../')

from zbus import RpcService, RpcServiceConfig, ServiceHandler, Message
import time

class MyServiceHandler(ServiceHandler):
    def handle_request(self, msg): 
        print msg
        res = Message()
        res.set_status('200')
        res.set_body('hello server@%s'%time.time())
        return res
        
handler = MyServiceHandler() 
 
config = RpcServiceConfig()
config.broker = '127.0.0.1'
config.service_name = 'MyRpc'
config.service_andler = handler

svc = RpcService(config)
svc.start()
svc.join()




