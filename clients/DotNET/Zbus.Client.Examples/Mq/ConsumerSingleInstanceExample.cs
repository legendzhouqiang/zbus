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
   class ConsumerSingleInstanceExample
   {  
      static void Main(string[] args)
      { 
         IBroker broker = new ZbusBroker("127.0.0.1:15555");
         Consumer consumer = new Consumer(broker, "MyMQ");

         consumer.MessageReceived += (msg, csm) =>
         {
            Console.WriteLine(msg);
         };

         consumer.Start();
      }
   }
}
