from zbus import MqClient, Message

client = MqClient("localhost:15555")
client.connect()

msg = Message()
msg.topic = 'hong'
msg.body = 'hello world'
client.produce(msg)
res = client.consume('hong') 
print (res) 


client.close()