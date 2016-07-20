#encoding=utf8

from zbus import Service,ServiceConfig, RpcServiceHandler, Remote, SingleBroker

class MyService(object): 
    @Remote()
    def getString(self, ping):
        return ping
    
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
 
handler = RpcServiceHandler()
handler.add_module('Interface', MyService())


config = ServiceConfig()
broker = SingleBroker(host = 'localhost', port = 15555)
config.broker = broker 
config.service_name = 'MyRpc'
config.service_andler = handler
config.thread_count = 20

svc = Service(config)
svc.start()
svc.join()




