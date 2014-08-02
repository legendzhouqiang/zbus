#encoding=utf8
import sys 
sys.path.append('../../')

from zbus import RemotingClient, Rpc, Message
client = RemotingClient(broker='127.0.0.1:15555')

rpc = Rpc(client=client, 
          mq='MyRpc') 

for i in range(10):
    msg = Message()
    msg.set_body('hello?')
    res = rpc.invoke(msg)
    print res
    
client.close()




