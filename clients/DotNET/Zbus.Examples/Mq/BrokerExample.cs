using System;
using System.Threading.Tasks;
using Zbus.Mq;

namespace Zbus.Examples
{
    class BrokerExample
    {
        static async Task Test()
        {
            Broker broker = new Broker();
            await broker.AddServerAsync("localhost:15555");
        }
        static void Main(string[] args)
        {
            Test().Wait();
            Console.ReadKey();
        }
    }
}
