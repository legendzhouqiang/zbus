#encoding=utf8
import sys 
sys.path.append('../') 

from zbus import Message, RemotingClient, Producer
 
client = RemotingClient(broker='127.0.0.1:15555')

#Producer是轻量级对象,不需要关闭
p = Producer(client=client, mq='MyMQ') 

for i in range(10):
    msg = Message()    
    msg.set_body('hello world %s'%i) 
    print p.send(msg)


client.close()



