#encoding=utf8
import sys 
sys.path.append('../../') 

from zbus import SingleBroker, Rpc 

broker = SingleBroker(host='127.0.0.1', port=15555)

rpc = Rpc(broker=broker, 
          mq='MyRpc', 
          module='Interface')

for i in range(1000):
    print rpc.plus(1,2)

broker.destroy()






