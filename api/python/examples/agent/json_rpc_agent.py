#encoding=utf8
import sys 
sys.path.append('../../') 

from zbus import JsonRpc, ClientAgent

agent = ClientAgent(track_server='127.0.0.1:16666;127.0.0.1:16667')

rpc = JsonRpc(client_pool = agent, 
              mq = 'MyJsonRpc', 
              module = 'ServiceInterface')

for i in range(100):
    print rpc.plus(1,2)








