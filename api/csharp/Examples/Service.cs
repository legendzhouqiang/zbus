using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;


using Zbus.Mq;
using Zbus.Net;
using Zbus.Broker;
using Zbus.Rpc;

namespace Zbus.Examples
{
    class ServiceTest
    {
        class MyServiceHandler : ServiceHandler
        {
            public Message HandleRequest(Message request)
            {
                request.SetBody("response from C#");
                return request;
            }
        }

        public static void Main(string[] args)
        {

            BrokerConfig config = new BrokerConfig();
            config.brokerAddress = "127.0.0.1:15555";
            Broker broker = new SingleBroker(config);

            ServiceConfig serviceConfig = new ServiceConfig(broker);
            serviceConfig.mq = "MyService";
            serviceConfig.serviceHandler = new MyServiceHandler();

            Service service = new Service(serviceConfig);
            service.Start();
        }
    }
}
