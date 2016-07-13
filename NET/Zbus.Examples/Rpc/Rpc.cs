using Zbus.Broker;
using Zbus.RPC;
using System;
namespace Zbus.Examples
{
   class RpcClientTest
   {
      public static void Main(string[] args)
      {
         IBroker broker = new ZbusBroker();
         Rpc rpc = new Rpc(broker);

         Request req = new Request();
         req.Mq = "MyRpc";
         req.Module = "MyService";
         req.Method = "getString";
         req.Params = new object[] { "test" };


         object res = rpc.Invoke(req);
         Console.WriteLine(res);

         broker.Dispose();
         Console.ReadKey();
      }
   }
}
