#encoding=utf8
import sys 
sys.path.append('../../') #增加zbus路径

from zbus import ClientAgent, Consumer 

#ClientAgent实现了IClientBuilder
agent = ClientAgent(track_server='127.0.0.1:16666;127.0.0.1:16667')

consumer = Consumer(client_builder=agent, 
                    mq='MyMQ') 
i = 0
while True:
    msg = consumer.recv()
    if msg is None:
        continue
    i += 1
    print '======================%s======================'%i
    print msg
     
    
    
    




