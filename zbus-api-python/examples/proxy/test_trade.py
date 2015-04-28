#encoding=utf8 
from zbus import Trade, gen_trade_param
from zbus import SingleBroker
 

#创建一个Broker（管理链接，抽象zbus节点。 高可用版本可直接换HaBroker即可）
broker = SingleBroker(host='localhost', port=15555)
trade = Trade(broker=broker, mq='Trade') 


t = gen_trade_param(func_no='421324', 
                    ip_address='127.0.0.1', 
                    trade_node='9501', 
                    auth_id='' , 
                    branch_code='1100' , 
                    login_id='110002377535')

t['params'] = ['110002377535']  

print trade.request(t) 
 
print trade.encrypt('KDE', '123456', '110000001804') #q+4ooPSsA5oAnx+fwv2k4g==
password = 'IKNSg6K7twOgLcqnouCKePNyv3XJbiCmS6esaMr+uTZr8c9RTgc/YwsivG35Yc1UWUf9q5wFqetLkE0vdoUg4I=='
print trade.decrypt(password)

