#encoding=utf8
import sys 
sys.path.append('../../') 
sys.path.append('../') 

from zbus import SingleBroker, Rpc 

broker = SingleBroker(host='127.0.0.1', port=15555)

#纯粹简单的Rpc调用，无特殊协议要求
rpc = Rpc(broker=broker, 
          mq='KCXP',  
          encoding='utf8',
          timeout=10)

from proxy import KcxpRequest, KcxpResult
func_id = 'L0063001'
params = KcxpRequest(func_id)
#params.gen_common_params()

params["BROKER_CODE"] = "110050000012"
params["ORG_TYPE"] = "0"
params["INT_ORG"] = "1100"
 
rpc.method = func_id
rpc.encoding = 'gbk'
j = rpc.invoke([params])

res = KcxpResult.from_json(j)
print res


broker.destroy()
