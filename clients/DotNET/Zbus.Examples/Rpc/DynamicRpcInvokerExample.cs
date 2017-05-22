using Api.Example;
using log4net;
using System;
using System.Threading.Tasks;
using Zbus.Mq;
using Zbus.Rpc;

namespace Zbus.Examples
{
    class DynamicRpcInvokerExample
    {
        static void Main(string[] args)
        {
            Broker broker = new Broker();
            broker.AddTracker("localhost:15555");

            dynamic rpc = new RpcInvoker(broker, "MyRpc");

            var res = rpc.plus(1, 2); //magic!!!, just like javascript 
            Console.WriteLine(res);


            Console.WriteLine("done");
            Console.ReadKey();
        }
    }
}
