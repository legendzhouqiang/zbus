using log4net;
using System;
using System.Threading.Tasks;
using Zbus.Mq;

namespace Zbus.Examples
{
    class ConsumerExample
    {
        private static readonly ILog log = LogManager.GetLogger(typeof(ConsumerExample));
        static void Main(string[] args)
        {
            log.Info("begin test");

            Broker broker = new Broker();
            broker.AddTracker("localhost:15555");

            Consumer c = new Consumer(broker, "MyTopic");
            c.MessageReceived += (msg, client) => {
                Console.WriteLine(msg);
            };
            c.Start();

            Console.WriteLine("done");
            Console.ReadKey();
        }
    }
}
