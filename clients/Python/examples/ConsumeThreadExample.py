from zbus import MqClient, ConsumeThread

client = MqClient('localhost:15555')
client.connect()

def on_message(msg, client):
    print(msg)

t = ConsumeThread(client, 'MyTopic2')
t.on_message = on_message
t.start()