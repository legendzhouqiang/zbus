#encoding=utf8  
import httplib
 
httpClient = httplib.HTTPConnection('localhost', 15555, timeout=10)
headers = {
           'cmd': 'produce', 
           'mq': 'MyMQ'
        }
body = 'hello world from python HTTP'

httpClient.request('GET', '/', body=body, headers=headers)
 
print httpClient.getresponse().read() 

httpClient.close()

