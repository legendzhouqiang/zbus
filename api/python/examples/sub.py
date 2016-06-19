#encoding=utf8
from zbus import SingleBroker, Consumer, MqMode

broker = SingleBroker(host='localhost', port=15555)

consumer = Consumer(broker=broker,mq='MyPubSub',mode=MqMode.PubSub) 

consumer.topic = 'sse' #指定感兴趣的消息主题，用','分割不同主题

while True:
    msg = consumer.recv()
    if msg is None: continue
    print msg
     

broker.destroy()  




