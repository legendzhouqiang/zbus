from zbus import MqClientPool, Message

pool = MqClientPool("localhost:15555")

client = pool.borrow_client()
client.connect()

msg = Message()
msg.topic = 'hong'
msg.body = 'hello world'

client.produce(msg)
res = client.consume('hong') 
print (res) 

pool.return_client(client)
pool.close()