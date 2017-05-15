using Api.Example;
using System;
using System.Threading.Tasks;
using Zbus.Mq;
using Zbus.Mq.Net;
using Zbus.Rpc;
namespace Zbus.Examples
{
    class Program
    {

        static void ConsumerTest()
        {
            IBroker broker = new ZbusBroker("127.0.0.1:15555");

            Consumer consumer = new Consumer(broker, "MyMQ");

            consumer.MessageReceived += (msg, csm) =>
            {
                Console.WriteLine(msg);
            };

            consumer.Start();

        }

        static void MassiveConnect()
        {
            for (int i = 0; i < 10000; i++)
            {
                MessageClient client = new MessageClient();
                client.Connect("localhost:15555");
                Console.WriteLine(i + ":" + client.Active);
                client.Dispose();

            }

        }
        static void Test()
        {

            MessageClient client = new MessageClient();
            client.Connect("localhost:15555");

            Message req = new Message
            {
                Cmd = "consume",
                Mq = "MyMQ",
                BodyString = "hello world"
            };
            client.Send(req);
            Message res = client.Recv();

            client.Dispose();

            Console.WriteLine(res);
        }

        static async Task RpcTest()
        {
            RpcProcessor rpcProcessor = new RpcProcessor();
            rpcProcessor.AddModule<MyService>();

            IBroker broker = new ZbusBroker("127.0.0.1:15555");
            ConsumerServiceConfig config = new ConsumerServiceConfig
            {
                Mq = "MyRpc",
                Broker = broker,
                ConsumerCount = 4,
                ConsumerMessageHandler = rpcProcessor.OnConsumerMessage,
            };

            ConsumerService service = new ConsumerService(config);
            service.Start();

            RpcConfig rpcConfig = new RpcConfig
            {
                Broker = broker,
                ServiceId = "MyRpc",
            };

            IService s = RpcFactory.Create<IService>(rpcConfig);

            Console.WriteLine(s.Plus(1, 2));
            Console.WriteLine(await s.PlusAsync(1, 3));
            try
            {
                //s.ThrowException();
            }
            catch (NotImplementedException e)
            {
                Console.WriteLine(e);
            }


            RpcClient client = new RpcClient(broker, "MyRpc");

            int res = client.Invoke<int>("Plus", 1, 2);
            Console.WriteLine(res);

            res = (int)client.Invoke(typeof(int), "Plus", 1, 2);
            Console.WriteLine(res);

            client.Invoke("NoReturn");

            try
            {
                client.Invoke("Ignore");
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }

            try
            {
                client.Invoke("ThrowException");
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }


        }

        static void RpcClientTest()
        {
            using (IBroker broker = new ZbusBroker("127.0.0.1:15555"))
            {
                RpcClient client = new RpcClient(broker, "MyRpc");

                dynamic res = client.Invoke<int>("plus", 1, 2);
                Console.WriteLine(res);

                for (int i = 0; i < 100; i++)
                {
                    res = client.Invoke<string>("echo", "test");
                    Console.WriteLine(res);
                }
            }
        }

        static void Main(string[] args)
        {

            RpcTest().Wait();

            Console.WriteLine("===done===");
            Console.ReadKey();
        }
    }
}
