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
   class ProducerExample
   {  
      static void Main(string[] args)
      {
         using (IBroker broker = new ZbusBroker("127.0.0.1:15555"))
         {
            Producer p = new Producer(broker, "MyMQ");
            Message msg = new Message
            {
               BodyString = "hello world"
            };
            Message res = p.Send(msg); ;

            Console.WriteLine(res); 
         }

         Console.ReadKey();
      }
   }
}
