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
   class ConsumerMultiInstanceExample
   { 
      static void Main(string[] args)
      { 
         IBroker broker = new ZbusBroker("127.0.0.1:15555");

         ConsumerServiceConfig config = new ConsumerServiceConfig
         {
            Mq = "MyMQ",
            Broker = broker,
            ConsumerCount = 4, //Number of consumers to fetch message concurrently
            ConsumerMessageHandler = (msg, csm) =>
            {
               Console.WriteLine(msg);
            }
         };


         ConsumerService service = new ConsumerService(config);
         service.Start(); 
      }
   }
}
