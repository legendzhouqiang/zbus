#encoding=utf8
from zbus import SingleBroker, Producer, Message

broker = SingleBroker(host='localhost', port=15555)

#Producer是轻量级对象,不需要关闭
p = Producer(broker=broker, mq='MyPubSub') 
 
msg = Message()  
msg.set_topic('sse')  
msg.set_body('hello world, from python') 
print p.send(msg)

broker.destroy()




