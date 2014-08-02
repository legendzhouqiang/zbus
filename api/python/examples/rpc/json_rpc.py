#encoding=utf8
import sys 
sys.path.append('../../') 

from zbus import RemotingClient,JsonRpc 

client = RemotingClient(broker='127.0.0.1:15555')

rpc = JsonRpc(client=client, 
              mq='MyJsonRpc', 
              module='ServiceInterface')

print rpc.plus(1,2)








