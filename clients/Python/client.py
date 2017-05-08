import asyncio

class EventDriver(object):
    def __init__(self, loop):
        self.loop = loop

class MqClient(asyncio.Protocol):
    def __init__(self, event_driver):
        self.loop = event_driver.loop
        self.connected = None
        self.disconnected = None
        
    def connect(self, host, port):
        co = self.loop.create_connection(lambda: self, host, port)
        loop.run_until_complete(co)

    def connection_made(self, transport):
        self.socket = transport
        if self.connected:
            self.connected(self)

    def data_received(self, data):
        print('Data received: {!r}'.format(data.decode()))

    def connection_lost(self, exc):
        if self.disconnected:
            self.disconnected()

loop = asyncio.get_event_loop()
event_driver = EventDriver(loop)
client = MqClient(event_driver)

def func(c):  
    c.socket.write('hello'.encode())
    
client.connected = func

client.connect('localhost', 8888)
 
loop.run_forever()
loop.close()