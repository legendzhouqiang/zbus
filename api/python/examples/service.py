#encoding=utf8

from zbus import Service, ServiceConfig, ServiceHandler, Message, SingleBroker
import time

class MyServiceHandler(ServiceHandler):
    def handle_request(self, msg): 
        print msg
        res = Message()
        res.set_status('200')
        res.set_body('hello server@%s'%time.time())
        return res
 
config = ServiceConfig()

broker = SingleBroker(host='localhost', port=15555)
config.broker = broker
config.service_name = 'MyService'
config.thread_count = 1
config.service_andler = MyServiceHandler()


svc = Service(config)
svc.start()
svc.join()

broker.destroy()




