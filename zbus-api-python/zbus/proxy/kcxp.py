#encoding=utf8
import sys 
sys.path.append('../')

from zbus import Caller, Message
import json, base64

#kcxp公共参数封装
def gen_kcxp_param(func_no=None , ip_address = None , channel='g' , operway='g'):
    parms = {}
    #----common KCXP公共参数封装辅助类 -- 
    parms['F_FUNCTION'] = func_no 
    parms['F_SUBSYS'] = '1' 
    parms['F_OP_USER'] = '8888' 
    parms['F_OP_ROLE'] = '2'  
    parms['F_OP_SITE'] = ip_address  
    parms['F_CHANNEL'] = channel
    parms['F_SESSION'] = ''
    parms['F_RUNTIME'] =  ''
    parms['F_REMOTE_OP_ORG'] =''
    parms['F_REMOTE_OP_USER'] = ''
    parms['OP_USER'] = '8888'
    
    #----common g开头公共入参 --
    parms['g_serverid'] = '1'
    parms['g_funcid'] = func_no
    parms['g_operid'] = '8888'
    parms['g_operpwd'] = ''
    parms['g_operway'] = operway
    parms['g_stationaddr'] = ip_address
    parms['g_checksno'] = ''
    return parms


class Kcxp(Caller):
    
    def __init__(self, broker=None, mq = None, access_token='',
                 register_token='', mehtod=None, 
                 timeout=10, encoding='gbk'):
        Caller.__init__(self, broker=broker, mq = mq, access_token=access_token,
                 register_token=register_token ) 
        self.encoding = encoding

    
    def request(self, args, timeout=10):
        method = args['F_FUNCTION']  
        
        json_req = {'method': method, 'params': [args]} 
        msg = Message()
        msg.set_json_body(json.dumps(json_req, encoding=self.encoding))
        
        rtn_result = {'rtnCode':'1' , 'rtnMsg':'request success!'}
        res = self.invoke(msg, timeout=10)
        if res is None:
            rtn_result = {'rtnCode':'-1' , 'rtnMsg':'request timeout'}
        json_body = json.loads(res.body, self.encoding) 
        if 'result' in json_body:
            rs_array_json = json_body['result'] #多结果集
            rs_array = []
            for rs_json in rs_array_json: #单个结果集
                rs = []
                for row_json in rs_json: #结果集的每条记录
                    row = {}
                    for k,v in row_json.iteritems(): 
                        v = base64.decodestring(v) 
                        row[k] = v.decode(self.encoding) 
                    rs.append(row)
                rs_array.append(rs) 
                
            rtn_result['list'] = rs_array
            rtn_result['res_code'] = '200'
            rtn_result['raw_code'] = '0'
            rtn_result['raw_msg'] = u'成功!'
        else:
            rtn_result = {'rtnCode':'-1' , 'rtnMsg':'request fail'};
            if 'error_code' in json_body:
                error_code = json_body['error_code']
                rtn_result['res_code'] = error_code
                rtn_result['raw_code'] = error_code
                if 'error_msg' in json_body:
                    rtn_result['raw_msg'] = json_body['error_msg']
                else:
                    rtn_result['raw_msg'] = u'失败'
                    
        return rtn_result

__all__ = [
    Kcxp, gen_kcxp_param
]    

