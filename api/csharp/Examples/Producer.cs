using System; 

using Zbus.Mq;
using Zbus.Net;
using Zbus.Broker; 

namespace Zbus.Examples
{
    public class ProducerTest
    {
        public static void Main(string[] args)
        { 
            //1) create a broker to zbus(using BrokerConfig to change default)
            IBroker broker = new SingleBroker();
            //2) create a producer with broker and MQ name in ZBUS
            Producer producer = new Producer(broker, "MyMQ");
            //3) create MQ if needed
            producer.CreateMQ(); 

            Message msg = new Message(); 
            msg.SetBody("hello world from C# {0}", DateTime.Now);
            msg = producer.Send(msg, 10000); //timeout for waiting reply from zbus

            //4) dispose the broker
            broker.Dispose();
            Console.ReadKey();
        }
    }
}
