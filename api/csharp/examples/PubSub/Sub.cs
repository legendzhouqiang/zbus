using Zbus.Net;
using Zbus.Mq;
using Zbus.Broker;

namespace Zbus.Examples.PubSub
{

    class MyHandler : IMessageHandler
    {
        public void Handle(Message msg, Consumer consumer)
        {
            System.Console.WriteLine(msg);
        }
    }

    class Sub
    {
        public static void Main(string[] args)
        {  
            IBroker broker = new SingleBroker(); //using BrokerConfig to change default

            Consumer c = new Consumer(broker, "MyPubSub", MqMode.PubSub);
            c.ConsumeTimeout = 30000;
            c.Topic = "zbus";

            c.OnMessage(new MyHandler());
            c.Start();
            //c.Stop();

            //broker.Dispose();
        }
    }
}
