#encoding=utf8
import sys 
sys.path.append('../')

from zbus import RemotingClient, Consumer, MessageMode

client = RemotingClient(broker = '127.0.0.1:15555')

consumer = Consumer(client=client, 
                    mq='MySub', 
                    mode=MessageMode.PubSub)#指定消息模式
consumer.topic = 'qhee,xmee' #指定感兴趣的消息主题，用','分割不同主题

while True:
    msg = consumer.recv()
    if msg is None: continue
    print msg
     
    




