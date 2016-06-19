#encoding=utf8 
import sys 
sys.path.append('../') 
 
import httplib
import json
#填写zbus服务地址（zbus直接支持HTTP）
httpClient = httplib.HTTPConnection('localhost', 15555, timeout=10)

#zbus扩展命令--HTTP头部
headers = {
    'cmd': 'produce', 
    'mq': 'KCXP',
    'ack': False,
}

#method+params_array, json格式
func_id = 'L0063001'


from proxy import KcxpRequest, KcxpResult
params = KcxpRequest(func_id)
params.gen_common_params()

params["BROKER_CODE"] = "110050000012"
params["ORG_TYPE"] = "0"
params["INT_ORG"] = "1100"

req = {
    'method': 'L0063001',
    'params': [params]
}

httpClient.request('GET', '/', body=json.dumps(req, encoding='gbk'), headers=headers)
res_str = httpClient.getresponse().read() 

res = KcxpResult.from_string(res_str)

print res

httpClient.close()

