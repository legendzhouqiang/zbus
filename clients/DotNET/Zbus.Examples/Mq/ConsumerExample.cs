using System;
using System.Threading.Tasks;
using Zbus.Mq;

namespace Zbus.Examples
{
    class ConsumerExample
    {
        static async Task Test()
        {
            MqClient client = new MqClient("localhost:15555");
            await client.ConnectAsync();

            Message msg = await client.ConsumeAsync("hong5");
            Console.WriteLine(msg.Headers); 
        }
        static void Main(string[] args)
        {
            Test().Wait();
            Console.ReadKey();
        }
    }
}
