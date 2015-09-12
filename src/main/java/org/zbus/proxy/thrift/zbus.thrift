namespace java org.zbus.thrift  
service ZbusApi{ 
  string createMq(1:string mq, 2:i32 mode);
  string produce(1:string mq, 2:string msg);
  string consume(1:string mq, 2:i32 timeout);
  string rpc(1:string mq, 2:string rpc_param_json);
}