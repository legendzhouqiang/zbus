using Api.Example;
using log4net;
using System;
using System.Threading.Tasks;
using Zbus.Mq;
using Zbus.Rpc;

namespace Zbus.Examples
{
    class RpcProcessorExample
    { 
        static void Main(string[] args)
        { 
            RpcProcessor p = new RpcProcessor();
            p.AddModule<MyService>();


            Broker broker = new Broker();
            broker.AddTracker("localhost:15555");

            Consumer c = new Consumer(broker, "MyRpc");
            c.MessageReceived += p.MessageHandler;
            c.Start();

            Console.WriteLine("done");
            Console.ReadKey();
        }
    }
}
