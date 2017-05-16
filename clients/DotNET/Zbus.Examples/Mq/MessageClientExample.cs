using System;
using Zbus.Mq.Net;
using Zbus.Mq;

namespace Zbus.Examples
{
    class MessageClientExample
    {
        static void Main(string[] args)
        {
            MessageClient client = new MessageClient("localhost:15555");
            client.Connect();

            Message msg = new Message
            {
                Url = "/",
            };

            Message res = client.Invoke(msg);
            Console.WriteLine(res);


            Console.ReadKey();
        }
    }
}
