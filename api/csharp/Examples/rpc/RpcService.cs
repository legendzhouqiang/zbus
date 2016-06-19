using System;  

using Zbus.Broker;
using Zbus.RPC;

namespace Zbus.Examples
{
    class User
    {
        public string name;
        public string addr;
    }

    class MyService
    { 
        public string getString(string msg)
        {
            return msg;
        }
         
        public int plus(int a, int b)
        {
            return a + b;
        }
         
        public User user(string name)
        {
            User user = new User();
            user.name = name;
            user.addr = "深圳";
            return user; 
        }

        [Remote(Exclude=true)]
        public void ignore()
        {

        }
    }

    class MyService2
    { 
        public string getString(string msg)
        {
            return msg;
        } 
    }
    
    
    class RpcServiceTest
    {
        public static void Main(string[] args)
        { 
            IBroker broker = new SingleBroker(); //using default configuration

            RpcProcessor processor = new RpcProcessor();
            processor.AddModule(new MyService());
            processor.AddModule(new MyService2());

            ServiceConfig config = new ServiceConfig(broker);
            config.Mq = "MyRpc"; //Service entry in zbus as a MQ
            config.MessageProcessor = processor;
            config.ConsumerCount = 32;
 
            Service service = new Service(config);
            service.Start();
            //service.Stop();
            
            //broker.Dispose(); 
            
            //Console.ReadKey();
        } 
    }
}
