#encoding=utf8
import sys 
sys.path.append('../../') 

from zbus import Message, Producer, ClientAgent
 
agent = ClientAgent(track_server='127.0.0.1:16666;127.0.0.1:16667')

p = Producer(client_pool=agent, 
             mq='MyMQ')

for i in range(10):
    msg = Message()    
    msg.set_body('hello world %s'%i) 
    print p.send(msg)

agent.close()


