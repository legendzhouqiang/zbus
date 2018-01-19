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
- 清理zbus7老版本协议适配代码 
- 服务器接受消息认证后删除Token敏感信息
- 增加支持排他消费分组
- 增加支持不指定消费分组创建动态新分组
- RPC设计独立于MQ，少量更改增加支持不经过MQ的HTTP RPC
- HTTP proxy在高压下偶尔堵塞的bug修复,保持长连接
- RPC方法列表支持URL访问

## 0.10.1 notes
- 增加zbus.js支持HTTP直接RPC调用
- FileKit增加Cache开关
- 默认RPC方法元信息页面修复module不准确的bug

## 0.10.2 notes

- MessageLogger增强，覆盖消费者所有消息，可选择过滤
- 增加支持RPC返回结果是否带有类型信息，以良好支持JAVA泛型。默认不返回JSON类型信息（浏览器友好）

## 0.11.0 notes

- RpcInvoker修复module指定错误
- TcpProxy，HttpProxy可随zbus带起来，在同一个进程中运行
- RPC增加Verbose选项，方便开发状态console查看消息
- MQ消费者端增加ACK确认,以及可配置的超时重发机制
- MQ数据格式增加checksum完整性验证
- 监控端口与主接入端口可分离，方便监控隔离

## 0.11.1 notes

- TcpProxy支持多目标配置
- QueueNak修改依赖JDK8，依赖JDK6

## 0.11.2 notes

- RPC默认返回application/json类型
- RPC支持错误信息显示开关，可隐藏堆栈信息
- RPC支持方法列表页面开关，可隐藏展示所有的方法信息
- 修复timeout类型bug，int=>long

## 0.11.3 notes

- 修复checksum缓存错误


## 0.11.4 notes

- RPC增加默认根目录页面配置，增加模块目录页面支持 
- 消息日志记录接口增加Session上下文， by @云风叶凡
- 增加对文件上传的消息类型支持, RPC中可以直接获取到FileForm详细上传信息

## 0.11.5 notes

- zbus.js支持RPC默认不填写服务器地址
- 服务器客户端都支持消息header部分URLEncode/Decode
- 修复RPC客户端只支持Runtime异常，支持各类异常


## TODO lists
- 删除消费分组crash JVM
