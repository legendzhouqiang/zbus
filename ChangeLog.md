# Change Log

## 0.9.1

- io.zbus.transport.http.MessageCodec remove default Content-Type
- MqClient.unconsume get ack from server
- HttpProxy add configurable hearbeat interval to target server
- zbus-dist/conf/zbus.xml default to no trackerList

## 0.9.2

- RpcCodec return correct json+charset

## 0.9.3

- MqClient.unconsume change to async with no return
- RpcCodec.encodeResponse remove class info
- fix SpringServiceBootstrap ssl config bug