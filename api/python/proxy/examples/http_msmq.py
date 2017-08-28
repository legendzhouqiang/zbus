#encoding=utf8  
import httplib
import json
#填写zbus服务地址（zbus直接支持HTTP）
httpClient = httplib.HTTPConnection('localhost', 15555, timeout=10)

#zbus扩展命令--HTTP头部
headers = {
    'cmd': 'produce', 
    'mq': 'MSMQ',
    'ack': False,
}

def http_invoke(req): 
    httpClient.request('GET', '/', body=json.dumps(req), headers=headers)
    return httpClient.getresponse().read() 


#method+params_array, json格式
req = {
    'method': 'encrypt',
    'params': ["KDE", "123456", "110000001804"]
}
print http_invoke(req)

from proxy import MsmqRequest as Request
password = 'IKNSg6K7twOgLcqnouCKePNyv3XJbiCmS6esaMr+uTZr8c9RTgc/YwsivG35Yc1UWUf9q5wFqetLkE0vdoUg4I=='
req = {
    'method': 'decrypt',
    'params': [Request.ALGORITHM, Request.PUBLIC_KEY, Request.PRIVATE_KEY, password]
}
print http_invoke(req)


req = Request(func_no='421324', 
              trade_node='9501', 
              auth_id='' , 
              branch_code='1100' , 
              login_id='110002377535')

req['params'] = ['110002377535']   

req = {
    'method': 'call',
    'params': [req.string_value()]
}
print http_invoke(req)

httpClient.close()

