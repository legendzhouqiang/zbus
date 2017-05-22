using Api.Example;
using log4net;
using System;
using System.Threading.Tasks;
using Zbus.Mq;
using Zbus.Rpc;

namespace Zbus.Examples
{
    class RpcFactoryExample
    {  
        static void Main(string[] args)
        {
            Broker broker = new Broker();
            broker.AddTracker("localhost:15555"); 
            RpcInvoker rpc = new RpcInvoker(broker, "MyRpc");


            IService svc = RpcFactory.Create<IService>(rpc);

            int res = svc.PlusAsync(1, 2).Result;

            Console.WriteLine(res);
            Console.ReadKey();
        }
    }
}
