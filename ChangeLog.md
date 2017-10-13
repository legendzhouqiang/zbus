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
- fix Consumer.pause/resume bug
- HttpProxy support short connection to target


## 0.9.3-release notes
- HTTP MessageCodec去除了默认的Content-Type设置
- RpcCodec正确返回JSON+Charset的Content-Type
- RpcCodec.encodeResponse兼容标准JSON去除了fastjson中的type信息
- MqAdaptor.unconsume默认不返回，MqClient.unconsume默认异步方式发送
- 修复Consumer.pause/resume多线程bug
- 修复SpringServiceBootstrap中SSL设置bug
- HttpProxy增强动态监测目标是否支持消息匹配，支持同步异步代理


## 0.9.4-release notes
- Message HTTP method reserved with header=origin-method
- fix recvFilter bug