using System;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Mq;

namespace Zbus.Examples
{
    class ConsumeThreadExample
    { 
        static void Main(string[] args)
        { 
            ConsumeThread thread = new ConsumeThread("hong")
            {
                MessageHandler = (msg, client) =>
                {
                    Console.WriteLine(JsonKit.SerializeObject(msg));
                },
                ClientFactory = () =>
                {
                    return new MqClient("localhost:15555");
                },
                ThreadCount = 2,
            };

            thread.Start();


            Console.WriteLine("done");
            Console.ReadKey();
        }
    }
}
