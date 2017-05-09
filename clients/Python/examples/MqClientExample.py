from zbus import MqClient 

client = MqClient("localhost:15555")

res = client.query_server()
print(res)