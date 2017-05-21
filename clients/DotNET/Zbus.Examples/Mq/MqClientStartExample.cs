using System;
using Zbus.Mq.Net;
using Zbus.Mq;
using System.Threading.Tasks;
using System.Threading;

namespace Zbus.Examples
{
    class MqClientStartExample
    { 

        static void Main(string[] args)
        {
            Test().Wait(); 

            Console.ReadKey();
        }

        static async Task Test()
        {
            MqClient client = new MqClient("localhost:15555");
            client.MessageReceived += (msg) =>
            {
                Console.WriteLine(JsonKit.SerializeObject(msg)); 
            };
            client.Connected += () =>
            {
                Console.WriteLine("connected");
            }; 
            client.Disconnected += () =>
            {
                Console.WriteLine("disconnected");
            };
            await client.ConnectAsync();
            client.Start();
            Message req = new Message
            {
                Cmd = Protocol.TRACK_SUB,
            };
            await client.SendAsync(req);
        }
    }
}
