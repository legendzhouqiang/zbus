using System;
using Zbus.Mq.Net;
using Zbus.Mq; 

namespace Zbus.Examples
{
    class MqClientExample
    {
        static void Main(string[] args)
        { 
            MqClient client = new MqClient("localhost:15555");  
            client.Connect();

            ServerInfo info = client.QueryServer();

            Console.WriteLine(info.TopicTable);

            Console.ReadKey();
        }
    }
}
