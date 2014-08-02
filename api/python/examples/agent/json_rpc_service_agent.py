#encoding=utf8
import sys 
sys.path.append('../../') #增加zbus路径


from zbus import RpcService, RpcServiceConfig, JsonServiceHandler, Remote
from zbus import ClientAgent

class MyService(object): 
    
    @Remote()
    def echo(self, ping):
        return ping
    
    @Remote()
    def save(self, user):
        print user
        return 'OK'
        
    @Remote('user')
    def user(self, username):
        return {'Name': username, 'Addr': u'中文'}
    
    @Remote()
    def plus(self, a, b):
        return a + b 

agent = ClientAgent(track_server='127.0.0.1:16666;127.0.0.1:16667')

handler = JsonServiceHandler()
handler.add_module('ServiceInterface', MyService())

config = RpcServiceConfig()
config.client_builder = agent
config.service_name = 'MyJsonRpc'
config.service_andler = handler
config.thread_count = 4

svc = RpcService(config)
svc.start()
svc.join()




