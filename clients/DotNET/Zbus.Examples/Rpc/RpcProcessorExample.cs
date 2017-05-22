using Api.Example;
using System;
using Zbus.Mq;
using Zbus.Rpc;

namespace Zbus.Examples
{
    class RpcProcessorExample
    { 
        static void Main(string[] args)
        { 
            RpcProcessor p = new RpcProcessor();
            p.AddModule<MyService>(); //Simple?


            Broker broker = new Broker("localhost:15555"); //Capable of HA failover, test it!
            Consumer c = new Consumer(broker, "MyRpc");
            c.MessageReceived += p.MessageHandler; //Set processor as message handler
            c.Start();

            Console.WriteLine("done");
            Console.ReadKey();
        }
    }
}
