#encoding=utf8  
import httplib
import json
httpClient = httplib.HTTPConnection('localhost', 15555, timeout=10)

#扩展HTTP头部
headers = {
    'cmd': 'produce', 
    'mq': 'MyRpc',
    'ack': False,
}
#JSON格式的HTTP消息体, method+params_list
req = {
    'module': 'Interface',
    'method': 'plus',
    'params': [1, 2]
}

httpClient.request('GET', '/', body=json.dumps(req), headers=headers)
 
print httpClient.getresponse().read() 

httpClient.close()

