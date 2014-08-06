#encoding=utf8
import sys 
sys.path.append('../') 

from zbus import RemotingClient, Consumer
client = RemotingClient(broker = '127.0.0.1:15555')
consumer = Consumer(client=client,mq='MyMQ')  
while True:
    msg = consumer.recv()
    if msg is None: continue
    print msg
    
    




