#encoding=utf8
import sys 
sys.path.append('../')

from zbus import RemotingClient, Consumer, MessageMode

client = RemotingClient(broker = '127.0.0.1:15555')

consumer = Consumer(client=client, 
                    mq='MySub', 
                    mode=MessageMode.PubSub) 

consumer.topic = 'qhee,xmee'
i = 0
while True:
    msg = consumer.recv()
    if msg is None:
        continue
    i += 1
    if i%1 == 0:
        print '================%s==================='%i
        print msg
     
    




