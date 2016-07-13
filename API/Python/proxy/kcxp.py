#encoding=utf8
class KcxpRequest(dict): 
    def __init__(self, func_no, ip='127.0.0.1'):
        self['F_FUNCTION'] = func_no
        self['F_OP_SITE'] = ip
    
    #KCXP公共参数封装辅助类
    def gen_fcommon_params(self):
        self["F_SUBSYS"] = "1"
        self["F_OP_USER"] = "8888"
        self["F_OP_ROLE"] = "2"
        self["F_CHANNEL"] = "g"
        self["F_SESSION"] = ""
        self["F_RUNTIME"] = ""
        self["F_REMOTE_OP_ORG"] = ""
        self["F_REMOTE_OP_USER"] = ""
        self["OP_USER"] = "8888"
    
    #g开头公共入参
    def gen_gcommon_params(self):
        self["g_serverid"] = "1" 
        self["g_operid"] = "8888"
        self["g_operpwd"] = ""
        self["g_operway"] = "g" 
        self["g_checksno"] = "" 
        
    def gen_common_params(self):
        self.gen_fcommon_params()
        self.gen_gcommon_params()
    

import json
import base64
class KcxpResult:
    def __init__(self):
        self.status = '0'
        self.error_message = ''
        self.tables = [] #[{resultset}, {[rows{},{},]}, {}]
    
    def __str__(self):
        if self.status != '0':
            return 'status=%s, error_message=%s'%(self.status, self.error_message)
        res = ''
        rs = 1
        for table in self.tables:
            res += 'ResultSet(%s):\n'%rs
            rs += 1
            for row in table:
                for key in row:
                    val = row[key]
                    res += "[%s=%s] "%(key.strip(), val)
                res += '\n' 
        if len(res) > 1:
            res = res[0: len(res)-1]
        return res
    
    @staticmethod  
    def from_string(string):
        j = json.loads(string, encoding='gbk')
        if 'error' in j:
            res = KcxpResult()
            res.status = j['error']
            if 'stackTrace' in j:
                res.error_message = j['stackTrace']
            return res
        return KcxpResult.from_json(j['result'])
    
    @staticmethod
    def from_json(j):
        res = KcxpResult()
        for jarray in j:
            table = []
            for jrow in jarray:
                row = {}
                for key in jrow:
                    val = jrow[key]
                    row[key]= base64.b64decode(val)  
                table.append(row)
            res.tables.append(table)
        return res

__all__ = [KcxpRequest, KcxpResult]    