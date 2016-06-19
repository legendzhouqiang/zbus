using System;
using System.Threading;
using System.Net.Sockets;

using Zbus.Net;
using Zbus.Mq;
using Zbus.Broker;

namespace Zbus.Examples
{ 
    class NetTest
    {
        public MessageClient client;
        public void Run()
        {
            Message msg = new Message();
            msg.Mq = "MyRpc";
            msg.Cmd = Proto.Consume;

            try
            { 
                Message res = client.Invoke(msg, 10000); 
                Console.WriteLine(res);
            }
            catch (Exception e)
            {
                Console.WriteLine(e.InnerException);
                Console.WriteLine(e);
            }
        }


        public static void Main(string[] args)
        {
            NetTest test = new NetTest();
            test.client = new MessageClient("127.0.0.1:15555");
            Thread t = new Thread(test.Run);
            t.Start();

            Thread.Sleep(1000); 
            test.client.Close();

            Console.ReadKey();
        }
    }
}
