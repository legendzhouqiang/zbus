using Zbus.Net;
using Zbus.Mq;
using Zbus.Broker;

namespace Zbus.Examples.MQ
{

    class MyHandler : IMessageHandler
    {
        public void Handle(Message msg, Consumer consumer)
        {
            System.Console.WriteLine(msg);
        }
    }

    class ConsumerTest
    {
        public static void Main(string[] args)
        {  
            IBroker broker = new SingleBroker(); //using BrokerConfig to change default

            Consumer c = new Consumer(broker, "MyMQ");
            c.ConsumeTimeout = 30000;

            c.OnMessage(new MyHandler());
            c.Start();
            //c.Stop();

            //broker.Dispose();
        }
    }
}
