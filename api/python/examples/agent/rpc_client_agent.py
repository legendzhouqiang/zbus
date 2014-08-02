#encoding=utf8
import sys 
sys.path.append('../../')

from zbus import ClientAgent ,Rpc, Message

agent = ClientAgent(track_server='127.0.0.1:16666;127.0.0.1:16667')


rpc = Rpc(client_pool=agent, 
          mq='MyRpc') 

for i in range(10):
    msg = Message()
    msg.set_body('hello?')
    res = rpc.invoke(msg)
    print res





