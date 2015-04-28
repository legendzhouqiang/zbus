#encoding=utf8
import sys 
sys.path.append('../')

from zbus import Caller, Message 
import json


#Trade公共参数封装
def gen_trade_param(func_no=None , ip_address=None , trade_node=None , auth_id=None , branch_code=None , login_id=None):
    t = {}
    t['funcId'] = func_no
    t['tradeNodeId'] = trade_node
    t['sessionId'] = ''
    t['userInfo'] = '0~hdpt~' + ip_address + '~' + branch_code 
    t['loginType'] = 'Z' 
    t['loginId'] = login_id
    t['custOrg'] = branch_code
    t['operIp'] = ip_address
    t['operOrg'] = branch_code
    t['operType'] = ''
    t['authId'] = ''
    t['accessIp'] = 'HDZX'
    t['extention1'] = '' 
    t['extention2'] = ''  
    return t

class Trade(Caller): 
    DO_CRYPT = '_do_crypt_'  
    DECRYPT_ALGORITHM = 'WANGAN'
    DECRYPT_PUBLIC_KEY = 'BF6C2C496593917FEEDFE0F6C62BA237C32A99886D66CC3D20DBAEB38484D001C86EE38576C6A92CA3C94C03B1AD284A0F85498D3DEB9134DFC57BABE8271401'
    DECRYPT_PRIVATE_KEY = '2D160168583065B8C83E9AF204C30A363015BC8BD198C0CA350F091AE73F90EE321E8767FED9CAA9FDD58960436B320FF4B7CFD06BFDA418D31290CA40DAE0F1'

    def __init__(self, broker=None, mq = None, access_token='',
                 register_token='', mehtod=None, 
                 timeout=10, encoding='gbk'):
        Caller.__init__(self, broker=broker, mq = mq, access_token=access_token,
                 register_token=register_token ) 
        self.timeout = timeout
        self.encoding = encoding   
        
    def encrypt(self, algorithm, password, key):
        json_req = {}
        json_req['method'] = 'encrypt'
        params = {}
        params['algorithm'] = algorithm
        params['password'] = password #base64.encodestring(password).strip()
        params['key'] = key
        json_req['params'] = params  
        
        return self._handle_crypt_result(json_req)
    
    def decrypt(self, password):
        return self.decrypt_full(self.DECRYPT_ALGORITHM, self.DECRYPT_PUBLIC_KEY, self.DECRYPT_PRIVATE_KEY, password)
    
    def decrypt_full(self, algorithm, publicKey, privateKey, password):
        json_req = {}
        json_req['method'] = 'decrypt'
        params = {}
        params['algorithm'] = algorithm
        params['public'] = publicKey
        params['private'] = privateKey
        params['password'] = password
        json_req['params'] = params
        
        return self._handle_crypt_result(json_req) 
    
    def _handle_crypt_result(self, json_req):
        req = Message()
        req.set_head(self.DO_CRYPT, '1') #合并后标识Trade服务当前是做加解密
        req.set_json_body(json.dumps(json_req))
       
        res = self.invoke(req, self.timeout)
        
        json_rep = json.loads(res.body)
        if 'result' in json_rep:
            return json_rep['result']
        if 'error_code' in json_rep:
            error_code = json_rep['error_code']
            error_msg = 'unknown'
            if 'error_msg' in json_rep:
                error_msg = json_rep['error_msg']
            raise Exception('error_code=%s, error_msg=%s'%(error_code, error_msg))
        else:
            raise Exception('unknown') 

    def request(self, args): 
        req_str = self.wrap_args(args)
        msg = Message()
        msg.set_body(req_str)
        res = self.invoke(msg, self.timeout) 
        if res is None:
            rtn_result = {'rtnCode':'-1' , 'rtnMsg':'request timeout'};
            return rtn_result
        
        rtn_result = {'rtnCode':'1' , 'rtnMsg':'request success!'}
        
        raw_msg = res.body  
        
        if isinstance(raw_msg, str):
            raw_msg = raw_msg.decode(self.encoding)
        #开始解析返回数据
        parts = raw_msg.split(';')
        
        #header
        headPart = parts[0]
        head = headPart.split('|')
        if len(head) < 5:
            rtn_result = {'rtnCode':'-1' , 'rtnMsg':'trade response head should be 5 parts(|)!'}
            
        rtn_result['funcId'] = head[0]
        rtn_result['tradeNodeId'] = head[1]
        rtn_result['userInfo'] = head[2]
        rtn_result['raw_code'] = head[3]
        rtn_result['raw_msg'] = head[4]
        #body
        if len(parts)>1:
            rtn_lst = []
            for i in range(1,len(parts)):
                body_part = parts[i]
                body_array = body_part.split('|')
                rtn_array = []
                for body in body_array:
                    if isinstance(body, str):
                        rtn_array.append(body.decode(self.encoding))
                    else:
                        rtn_array.append(body)
                rtn_lst.append(rtn_array)
            rtn_result['list'] = rtn_lst
        return rtn_result 
        
        
    #生成请求串
    def wrap_args(self , params = None):
        #head
        request_str = params['tradeNodeId']
        request_str += '|'+params['sessionId']
        request_str += '|'+params['funcId']
        request_str += '|'+params['userInfo'] 
        request_str += '|;'
        #body
        request_str += params['funcId']
        request_str += '|'+params['loginType']
        request_str += '|'+params['loginId']
        request_str += '|'+params['custOrg']
        request_str += '|'+params['operIp']
        request_str += '|'+params['operOrg']
        request_str += '|'+params['operType']
        request_str += '|'+params['authId']
        request_str += '|'+params['accessIp']
        request_str += '|'+params['extention1']
        request_str += '|'+params['extention2']
        #params
        if params.get('params') and len(params.get('params'))>0:
            for p in params.get('params'):
                if isinstance(p, str):
                    request_str += '|' + p.encode(self.encoding)
                else:
                    request_str += '|' + p
        return request_str
    
__all__ = [Trade, gen_trade_param]
