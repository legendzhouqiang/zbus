#encoding=utf8  
import httplib
 
http = httplib.HTTPConnection('localhost', 15555, timeout=100)
headers = {
           'cmd': 'consume', 
           'mq': 'MyMQ'
        } 
body = ''

http.request('GET', '/', body=body, headers=headers)
print http.getresponse().read() 

http.close()

