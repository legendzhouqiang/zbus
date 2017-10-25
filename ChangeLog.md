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
- 增加MessageLogger接口，可以个性化扩展日志记录

## 0.10.0 notes
- RPC增加原生HTTP消息返回，方便直接提供浏览器友好的HTTP服务
- RPC参数列表可以任意顺序插入Message请求参数的申明，获取RPC请求上下文
- 浏览器请求URL格式中module改为必填选，格式为 /{Topic}/{Module}/{Method}/{Param1}/{Param2}.....
- RPC结果返回状态改为由底层HTTP协议状态码控制：200正常，600业务逻辑错误
