from zbus import Broker, Consumer
 
broker = Broker()
broker.add_tracker('localhost:15555')


def on_message(msg, client):
    print(msg)

c = Consumer(broker, 'MyTopic')
c.on_message = on_message
c.connection_count = 2 
c.start()