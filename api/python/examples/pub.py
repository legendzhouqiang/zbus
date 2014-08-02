#encoding=utf8
import sys 
sys.path.append('../') 

from zbus import Message, RemotingClient, Producer, MessageMode
 
client = RemotingClient(broker='127.0.0.1:15555')

p = Producer(client=client, 
              mq='MySub',
              mode=MessageMode.PubSub)

for i in range(10):
    msg = Message()  
    msg.set_topic('qhee')  
    msg.set_body('hello world %s'%i) 
    print p.send(msg)

client.close()




