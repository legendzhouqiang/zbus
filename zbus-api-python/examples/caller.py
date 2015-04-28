#encoding=utf8

from zbus import Caller, Message, SingleBroker

broker = SingleBroker(host='localhost', port=15555)

c = Caller(broker=broker, mq='Trade') 

msg = Message()
msg.set_body('hello to msmq')
print c.invoke(msg) 

broker.destroy()




