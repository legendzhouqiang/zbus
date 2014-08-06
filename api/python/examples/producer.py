#encoding=utf8
import sys 
sys.path.append('../') 

from zbus import Message, RemotingClient, Producer
 
client = RemotingClient(broker='127.0.0.1:15555')

#Producer是轻量级对象,不需要关闭
p = Producer(client=client, mq='MyMQ') 

msg = Message()    
msg.set_body('hello world') 
print p.send(msg)


client.close()



