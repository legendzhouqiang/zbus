#encoding=utf8  
from zbus import SingleBroker, Consumer

broker = SingleBroker(host='localhost', port=15555)

consumer = Consumer(broker=broker, mq='MyMQ')  
while True:
    msg = consumer.recv()
    if msg is None: continue
    print msg
    
    




