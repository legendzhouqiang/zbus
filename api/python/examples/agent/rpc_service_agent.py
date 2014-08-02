#encoding=utf8
import sys 
sys.path.append('../../') #增加zbus路径

from zbus import RpcService, RpcServiceConfig, ServiceHandler, ClientAgent,\
    Message
import time

agent = ClientAgent(track_server='127.0.0.1:16666;127.0.0.1:16667')

class MyServiceHandler(ServiceHandler):
    def handle_request(self, msg):
        print msg
        res = Message() 
        res.set_status('200')
        res.set_body('hello server@%s'%time.time())
        return msg
        
handler = MyServiceHandler() 
 
config = RpcServiceConfig()
config.client_builder = agent #using track agent
config.service_name = 'MyRpc'
config.service_andler = handler

svc = RpcService(config)
svc.start()
svc.join()




