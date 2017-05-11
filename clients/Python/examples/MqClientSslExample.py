import ssl
import asyncio

@asyncio.coroutine
def tcp_echo_client(loop):
    sc = ssl.create_default_context(ssl.Purpose.SERVER_AUTH,
        cafile='/tmp/zbus.crt')

    reader, writer = yield from asyncio.open_connection('localhost', port=15555, ssl=sc, loop=loop)
    writer.write(b'ping\n')
    yield from writer.drain()
    data = yield from reader.readline()
    assert data == b'pong\n', repr(data)
    print("Client received {!r} from server".format(data))
    writer.close()
    print('Client done')

loop = asyncio.get_event_loop()
loop.run_until_complete(tcp_echo_client(loop))
loop.close()