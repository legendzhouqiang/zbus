using System;
using System.Threading;
using System.Reflection;
using System.Net.Sockets;

using Zbus.Kit.Log;
using Zbus.Mq;
using Zbus.Net;
using Zbus.Broker;

namespace Zbus.RPC
{ 
    class ServiceMessageHandler : IMessageHandler
    {
        private IMessageProcessor processor;
        private int routeTimeout = 10000;
        public ServiceMessageHandler(IMessageProcessor processor)
        {
            this.processor = processor;
        }
        public void Handle(Message req, Consumer consumer)
        {
            string sender = req.Sender;
            string msgId = req.Id;

            Message res = processor.Process(req);
            if (res != null)
            {
                res.Recver = sender;
                res.Id = msgId;
                if (res.Status == null)
                {
                    res.Status = "200";
                }
                consumer.Route(res, routeTimeout);
            }
        } 
    }
     

    public class Service : IDisposable
    {
        private static readonly ILogger log = LoggerFactory.GetLogger(typeof(Service));
        private ServiceConfig config;
        private Consumer[][] brokerConsumers;
        private bool started = false;
        public Service(ServiceConfig config)
        {
            this.config = config; 
        }

        public void Start()
        {
            if (started) return;
            started = true;
            IBroker[] brokers = config.Brokers;
            int consumerCount = config.ConsumerCount;
            if (brokers.Length < 1 || consumerCount < 1) return;

            this.brokerConsumers = new Consumer[brokers.Length][];
            for (int i = 0; i < brokers.Length; i++)
            {
                Consumer[] consumers = new Consumer[consumerCount];
                this.brokerConsumers[i] = consumers;
                for (int j = 0; j < consumerCount; j++)
                {
                    ServiceConfig myConfig = (ServiceConfig)this.config.Clone();
                    myConfig.Broker = brokers[i];
                    
                    Consumer consumer = new Consumer(myConfig);
                    IMessageHandler handler = new ServiceMessageHandler(myConfig.MessageProcessor);
                    consumer.OnMessage(handler);
                    consumer.Start();
                    consumers[j] = consumer; 
                }
            }

            log.InfoFormat("Service({0}) started", config.Mq);
        }

        public void Stop()
        {
            if (this.brokerConsumers != null)
            {
                for (int i = 0; i < brokerConsumers.Length; i++)
                {
                    Consumer[] consumers = brokerConsumers[i];
                    for (int j = 0; j < consumers.Length; j++)
                    {
                        consumers[j].Dispose();
                    }
                }
            }
            this.brokerConsumers = null;
            started = false;
        }

        public void Dispose()
        {
            Stop();
        }

        public bool Started
        {
            get{ return started; }
        }
    }

    class MethodInstance
    {
        private MethodInfo method;
        private object instance;

        public MethodInstance(MethodInfo method, object instance)
        {
            this.method = method;
            this.instance = instance;
        }

        public MethodInfo Method
        {
            get { return this.method; }
            set { this.method = value; }
        }

        public object Instance
        {
            get { return this.instance; }
            set { this.instance = value; }
        }
    } 

}
