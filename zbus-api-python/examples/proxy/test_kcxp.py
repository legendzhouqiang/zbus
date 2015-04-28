#encoding=utf8 

from zbus import Kcxp, gen_kcxp_param
from zbus import SingleBroker
 

#创建一个Broker（管理链接，抽象zbus节点。 高可用版本可直接换HaBroker即可）
broker = SingleBroker(host='localhost', port=15555)

kcxp = Kcxp(broker=broker, mq='KCXP') 


params = gen_kcxp_param(func_no='L0063001', ip_address = '172.24.174.52')
params["ORG_TYPE"] = "0"
params["ORG_CODE"] = "1100" 
res = kcxp.request(params) 
print res

