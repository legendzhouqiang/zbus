#encoding=utf8
from zbus import Caller, Message, SingleBroker

broker = SingleBroker(host='localhost', port=15555)

c = Caller(broker=broker, mq='MyService') 

msg = Message()
msg.set_body('hello from python')
print c.invoke(msg) 

broker.destroy()




