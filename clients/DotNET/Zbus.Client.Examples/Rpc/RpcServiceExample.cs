using Api.Example;
using Newtonsoft.Json;
using System;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Client.Broker;
using Zbus.Client.Mq;
using Zbus.Client.Net.Http;
using Zbus.Client.Rpc;

namespace Zbus.Client.Examples
{
   class RpcServiceExample
   { 
      static void Main(string[] args)
      { 
         RpcProcessor rpcProcessor = new RpcProcessor();
         rpcProcessor.AddModule<MyService>(); 

         IBroker broker = new ZbusBroker("127.0.0.1:15555");
         ConsumerServiceConfig config = new ConsumerServiceConfig
         {
            Mq = "MyRpc",
            Broker = broker,
            ConsumerCount = 1,
            ConsumerMessageHandler = rpcProcessor.OnConsumerMessage,
         }; 

         ConsumerService service = new ConsumerService(config);
         service.Start();

         Console.WriteLine("Service started");
      }
   }
}
