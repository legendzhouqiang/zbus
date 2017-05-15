import sys
sys.path.append("../")
from zbus import MqClientPool, Message

pool = MqClientPool("localhost:15555")
def on_connected(server_info):
    print(server_info)

pool.on_connected = on_connected
pool.start()


client = pool.borrow_client() 

msg = Message()
msg.topic = 'hong'
msg.body = 'hello world'

client.produce(msg)
res = client.consume('hong') 
print (res) 

pool.return_client(client)
pool.close()