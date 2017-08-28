#encoding=utf8
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from zbus import ZbusApi
import json
 
transport = TTransport.TFramedTransport(TSocket.TSocket('127.0.0.1', 25555))
protocol = TBinaryProtocol.TBinaryProtocol(transport)

transport.open()
client = ZbusApi.Client(protocol)

req = {
    'method': 'getOrder',
    #'params': ['testParamString']
}

for i in range(100):
    print client.rpc('MyRpc', json.dumps(req))

transport.close()



