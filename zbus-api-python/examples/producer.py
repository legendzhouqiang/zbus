#encoding=utf8

from zbus import SingleBroker, Producer, Message
broker = SingleBroker(host = 'localhost', port = 15555)

#Producer是轻量级对象,不需要关闭
p = Producer(broker=broker, mq='MyMQ') 

msg = Message()    
msg.set_mq_reply('MSMQ_REPLY')
msg.set_body('hello world') 
print p.send(msg)


broker.destroy()



