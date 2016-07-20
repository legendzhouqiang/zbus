
using Zbus.Mq;
using Zbus.Broker;
using Zbus.Net.Http;
using System.Text;
using System;
using System.Security.Cryptography;

namespace Zbus.Examples.Mq
{

   class MyHandler : IMessageHandler
   {
      public void Handle(Message msg, Consumer consumer)
      {
         System.Console.WriteLine(msg);
      }
   }

   class ConsumerTest
   { 
      public static void Main(string[] args)
      {
         IBroker broker = new ZbusBroker(); //using BrokerConfig to change default

         Consumer c = new Consumer(broker, "MyMQ");
         c.ConsumeTimeout = 30000;

         c.OnMessage(new MyHandler());
         c.Start();
         //c.Stop();

         //broker.Dispose();
      }
   }
}
