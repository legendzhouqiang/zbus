#encoding=utf8
import sys 
sys.path.append('../../') 

from zbus import SingleBroker, Rpc 

broker = SingleBroker(host='127.0.0.1', port=15555)

#纯粹简单的Rpc调用，无特殊协议要求
rpc = Rpc(broker=broker, 
          mq='MSMQ',  
          encoding='utf8',
          timeout=10)


from proxy import MsmqRequest as Request
print rpc.encrypt("KDE", "123456", "110000001804") #q+4ooPSsA5oAnx+fwv2k4g==

password = 'IKNSg6K7twOgLcqnouCKePNyv3XJbiCmS6esaMr+uTZr8c9RTgc/YwsivG35Yc1UWUf9q5wFqetLkE0vdoUg4I=='

print rpc.decrypt(Request.ALGORITHM, Request.PUBLIC_KEY, Request.PRIVATE_KEY, password)

req = Request(func_no='421324', 
        trade_node='9501', 
        auth_id='' , 
        branch_code='1100' , 
        login_id='110002377535')

req['params'] = ['110002377535']   

print rpc.call(req.string_value())


broker.destroy()





