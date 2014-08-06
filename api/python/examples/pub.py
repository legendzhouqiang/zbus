#encoding=utf8
import sys 
sys.path.append('../') 

from zbus import Message, RemotingClient, Producer, MessageMode
#整体与生产者几乎类似，除了指定消息模式为PubSub
client = RemotingClient(broker='127.0.0.1:15555')
p = Producer(client=client, 
              mq='MySub',
              mode=MessageMode.PubSub) #指定消息模式为发布订阅
 
msg = Message()  
msg.set_topic('qhee')  
msg.set_body('hello world') 
print p.send(msg)

client.close()




