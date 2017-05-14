from zbus import Broker, RpcInvoker
 
broker = Broker()
broker.add_tracker('localhost:15555')

rpc = RpcInvoker(broker, 'MyRpc')

res = rpc.invoke(method='plus', params=[1,2])
print(res)

res = rpc.plus(1,2)
print(res)

res = rpc.echo('hong')
print(res)
 
broker.close()