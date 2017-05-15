using System;
using System.IO;
using System.Net.Sockets;
using System.Threading;

using Zbus.Client.Broker;
using log4net;
using System.Threading.Tasks;
using Zbus.Client.Net;
using Zbus.Client.Mq.Net;

namespace Zbus.Client.Mq
{ 
   public delegate void ConsumerMessageHandler(Message msg, Consumer consumer);

   /// <summary> 
   /// 1) Call consumer.Take() directly, control the rate of consumption by the caller directly.
   /// 2) consumer.MessageReceived += handler, trigger consumer.start() to handle message
   /// </summary>
   public class Consumer : MqAdmin, IDisposable
   { 
      public string Topic { get; set; }
      public int ReconnectTimeout { get; set; } = 3000; //3 seconds  
      public int ConsumeWaitTimeout { get; set; } = 3000; //3 seconds  

      public event ConsumerMessageHandler MessageReceived; 

      private static readonly ILog log = LogManager.GetLogger(typeof(Consumer));
      private IMessageInvoker client = null;
      private volatile Thread consumerThread = null;  
      public Consumer(IBroker broker, String mq, params MqMode[] modes)
          : base(broker, mq, modes)
      {
      }

      public Consumer(MqConfig config)
          : base(config)
      {
         this.Topic = config.Topic;
      } 

      public Message Take(int timeout=10000)
      { 
         Message req = new Message();
         req.Cmd = Protocol.Consume;
         req.Mq = this.mq;
         if (this.Topic != null)
         {
            req.Topic = this.Topic;
         }

         try
         {
            if (this.client == null)
            {
               this.client = broker.GetInvoker(GetClientHint());
            }

            Message res = this.client.Invoke(req, timeout);
            if (res != null && res.Status == "404")
            {
               if (!this.CreateMq(timeout))
               {
                  throw new MqException("register error");
               }
               return Take(timeout);
            }
            if (res != null)
            {
               string originUrl = res.OriginUrl;
               if (originUrl != null)
               {
                  res.RemoveHead(Message.ORIGIN_URL);
                  res.Url = originUrl;
               }
               res.Id = res.OriginId;
               res.RemoveHead(Message.ORIGIN_ID);
            }
            return res;
         }
         catch (Exception ex)
         {
            Exception cause = ex.InnerException;
            if (cause == null) cause = ex;
            if (cause is SocketException) 
            {
               SocketException se = (SocketException)cause;
               if (se.SocketErrorCode == SocketError.TimedOut)
               { 
                  return null;
               } 

               this.HandleFailover();
               return null;
            }

            if (cause is IOException)
            { 
               this.HandleFailover();
               return null;
            }

            throw ex;
         }
      }

      public void Route(Message msg, int timeout=10000)
      {
         msg.Cmd = Protocol.Route;
         msg.Ack = false;
         if (msg.Status != null)
         {
            msg.OriginStatus = msg.Status;
            msg.Status = null;
         }

         if(!(client is IClient<Message, Message>))
         {
            throw new Exception("Consumer underlying invoker dot not support route function"); 
         }
         IClient<Message, Message> msgClient = (IClient<Message, Message>)client;
         msgClient.Send(msg, timeout);
      }

      protected override Message Invoke(Message req, int timeout = 10000)
      {
         return this.client.Invoke(req, timeout);
      }

      private void HandleFailover()
      {
         try
         {
            broker.CloseInvoker(this.client);
            this.client = broker.GetInvoker(GetClientHint());
         }
         catch (Exception ex)
         {
            Exception cause = ex.InnerException;
            if (cause == null) cause = ex;

            this.client = null; 
            string errorMsg = $"Trying to reconnect in {ReconnectTimeout} ms {cause.Message}";
            Console.WriteLine(errorMsg);
            log.Error(errorMsg);
            Thread.Sleep(ReconnectTimeout);
         } 
      } 

      private void Run()
      {
         while (true)
         {
            try
            {
               Message req = this.Take(ConsumeWaitTimeout);
               if (req == null) continue; 

               if (MessageReceived == null)
               { 
                  continue;
               }
               MessageReceived(req, this);
            }
            catch (ThreadInterruptedException)
            {
               Console.WriteLine("Consumer interrupted, exit consuming cycle.");
               break;
            }
            catch (Exception ex)
            {
               Console.WriteLine(ex);
               log.Error(ex.Message, ex);
               continue;
            }
         }

      }

      public void Start()
      {
         if (this.consumerThread == null)
         {
            this.consumerThread = new Thread(Run);
         }
         if (this.consumerThread.IsAlive) return;
         this.consumerThread.Start();
      }


      public void Stop()
      {
         if (this.consumerThread != null)
         {
            this.consumerThread.Interrupt();
            this.consumerThread = null;
         }
         if (this.client != null)
         {
            this.client.Dispose();  
         }
      }

      public void Dispose()
      {
         Stop();
      }
   }

   public class ConsumerServiceConfig : MqConfig
   {
      public ConsumerMessageHandler ConsumerMessageHandler { get; set; }
      public int ConsumerCount { get; set; } = 1;
      public int ReadTimeout { get; set; } = 3000;

      private IBroker[] brokers;
      public ConsumerServiceConfig(params IBroker[] brokers)
      {
         this.SetMode(MqMode.RPC);
         this.brokers = brokers;
         if (this.brokers.Length > 0)
         {
            this.Broker = this.brokers[0];
         }
      }

      public IBroker[] Brokers
      {
         get
         {
            if (this.brokers == null || this.brokers.Length == 0)
            {
               if (this.Broker != null)
               {
                  this.brokers = new IBroker[] { this.Broker };
               }
            }
            return this.brokers;
         }
      }
   }


   public class ConsumerService : IDisposable
   {
      private static readonly ILog log = LogManager.GetLogger(typeof(ConsumerService));
      private ConsumerServiceConfig config;
      private Consumer[][] brokerConsumers;
      private bool started = false;
      public ConsumerService(ConsumerServiceConfig config)
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

         ConsumerMessageHandler messageHandler = config.ConsumerMessageHandler;

         this.brokerConsumers = new Consumer[brokers.Length][];
         for (int i = 0; i < brokers.Length; i++)
         {
            Consumer[] consumers = new Consumer[consumerCount];
            this.brokerConsumers[i] = consumers;
            for (int j = 0; j < consumerCount; j++)
            {
               ConsumerServiceConfig myConfig = (ConsumerServiceConfig)this.config.Clone();
               myConfig.Broker = brokers[i];

               Consumer consumer = new Consumer(myConfig); 
               consumer.MessageReceived += (msg, csm) =>
               { 
                  messageHandler(msg, csm); 
               };

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
         get { return started; }
      }
   }

}
