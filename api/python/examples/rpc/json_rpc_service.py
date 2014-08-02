#encoding=utf8
import sys 
sys.path.append('../../') 


from zbus import RpcService, RpcServiceConfig, JsonServiceHandler, Remote

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
        print 'plus(%s,%s)'%(a, b)
        return a + b 
 
handler = JsonServiceHandler()
handler.add_module('ServiceInterface', MyService())

config = RpcServiceConfig()
config.broker = '127.0.0.1:15555'
config.service_name = 'MyJsonRpc'
config.service_andler = handler
config.thread_count = 1

svc = RpcService(config)
svc.start()
svc.join()




