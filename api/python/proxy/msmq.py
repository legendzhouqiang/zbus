#encoding=utf8
#Trade公共参数封装
class MsmqRequest(dict):
    ALGORITHM = "WANGAN"
    PUBLIC_KEY = "BF6C2C496593917FEEDFE0F6C62BA237C32A99886D66CC3D20DBAEB38484D001C86EE38576C6A92CA3C94C03B1AD284A0F85498D3DEB9134DFC57BABE8271401"
    PRIVATE_KEY = "2D160168583065B8C83E9AF204C30A363015BC8BD198C0CA350F091AE73F90EE321E8767FED9CAA9FDD58960436B320FF4B7CFD06BFDA418D31290CA40DAE0F1"
    def __init__(self, func_no=None , ip_address='127.0.0.1' , trade_node=None , 
                 auth_id=None , branch_code=None , login_id=None):
        self.encoding = 'gbk' #default to GBK
        
        self['funcId'] = func_no
        self['tradeNodeId'] = trade_node
        self['sessionId'] = ''
        self['userInfo'] = '0~hdpt~' + ip_address + '~' + branch_code 
        self['loginType'] = 'Z' 
        self['loginId'] = login_id
        self['custOrg'] = branch_code
        self['operIp'] = ip_address
        self['operOrg'] = branch_code
        self['operType'] = ''
        self['authId'] = ''
        self['accessIp'] = 'HDZX'
        self['extention1'] = '' 
        self['extention2'] = ''  
    
    #生成请求串 
    def __str__(self):
        request_str = self['tradeNodeId']
        request_str += '|'+self['sessionId']
        request_str += '|'+self['funcId']
        request_str += '|'+self['userInfo'] 
        request_str += '|;'
        #body
        request_str += self['funcId']
        request_str += '|'+self['loginType']
        request_str += '|'+self['loginId']
        request_str += '|'+self['custOrg']
        request_str += '|'+self['operIp']
        request_str += '|'+self['operOrg']
        request_str += '|'+self['operType']
        request_str += '|'+self['authId']
        request_str += '|'+self['accessIp']
        request_str += '|'+self['extention1']
        request_str += '|'+self['extention2']
        #params
        params = self.get("params")
        if params and len(params)>0:
            for p in params:
                if isinstance(p, str):
                    request_str += '|' + p.encode(self.encoding)
                else:
                    request_str += '|' + p
        return request_str

    def string_value(self):
        return self.__str__()

__all__ = [MsmqRequest]
