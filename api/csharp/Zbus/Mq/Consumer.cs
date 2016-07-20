using log4net;
using System;
using System.IO;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Broker;
using Zbus.Net.Http;

namespace Zbus.Mq
{
   public interface IMessageHandler
   {
      void Handle(Message msg, Consumer consumer);
   }
   public class Consumer : MqAdmin, IDisposable
   {
      private static readonly ILog log = LogManager.GetLogger(typeof(Consumer));
      private IMessageInvoker client = null;
      public string Topic { get; set; }
      public int ConsumeTimeout { get; set; } = 300000; //5 minutes
      public bool ConsumerHandlerRunInPool { get; set; } = true;

      public Consumer(IBroker broker, String mq, params MqMode[] modes)
          : base(broker, mq, modes)
      {
      }

      public Consumer(MqConfig config)
          : base(config)
      {
         this.Topic = config.Topic;
      }

      public Message Take()
      {
         Message msg = null;
         while (msg == null)
         {
            msg = Recv(ConsumeTimeout);
         }
         return msg;
      }

      /// <summary>
      /// 
      /// </summary>
      /// <param name="timeout"></param>
      /// <returns></returns>
      public Message Recv(int timeout)
      {
         if (this.client == null)
         {
            this.client = broker.GetInvoker(GetClientHint());
         }
         Message req = new Message();
         req.Cmd = Proto.Consume;
         req.Mq = this.mq;
         if ((this.mode & (int)MqMode.PubSub) != 0)
         {
            if (this.Topic != null)
            {
               req.Topic = this.Topic;
            }
         }
         try
         {
            Message res = this.client.InvokeSync(req, timeout);
            if (res != null && res.IsStatus404())
            {
               if (!this.CreateMQ())
               {
                  throw new MqException("register error");
               }
               return Recv(timeout);
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
         catch (IOException ex)
         {
            Exception cause = ex.InnerException;
            if (cause is SocketException)
            {
               SocketException se = (SocketException)cause;
               if (se.SocketErrorCode == SocketError.TimedOut)
               {
                  if (Environment.Version.Major < 4) //.net 3.5 socket sucks!!!!
                  {
                     this.HandleFailover();
                  }
                  return null;
               }

               if (se.SocketErrorCode == SocketError.Interrupted)
               {
                  throw se;
               }

               // all other socket error reconnect by default
               this.HandleFailover();
               return null;
            }

            throw ex;
         }
      }

      public void Route(Message msg)
      {
         msg.Cmd = Proto.Route;
         msg.Ack = false;
         if (msg.Status != null)
         {
            msg.OriginStatus = msg.Status;
            msg.Status = null;
         }

         this.client.InvokeAsync(msg);
      }


      private void HandleFailover()
      {
         try
         {
            broker.CloseInvoker(this.client);
            this.client = broker.GetInvoker(GetClientHint());
         }
         catch (IOException ex)
         {
            log.Error(ex.Message, ex);
         }

      }



      private volatile Thread consumerThread = null;
      private volatile IMessageHandler consumerHandler = null;

      public void OnMessage(IMessageHandler handler)
      {
         this.consumerHandler = handler;
      }


      private void Run()
      {
         while (true)
         {
            try
            {
               Message req = this.Recv(this.ConsumeTimeout);
               if (req == null) continue;

               if (consumerHandler == null)
               {
                  log.Warn("Missing consumer MessageHandler, call OnMessage first");
                  continue;
               }
               if (ConsumerHandlerRunInPool)
               {
                  Task.Run(() =>
                  {
                     consumerHandler.Handle(req, this);
                  });
               }
               else
               {
                  consumerHandler.Handle(req, this);
               }
               
            }
            catch (SocketException se)
            {
               if (se.SocketErrorCode == SocketError.Interrupted)
               {
                  break;
               }
               log.Error(se.Message, se);
            }
            catch (ThreadInterruptedException e)
            {
               break;
            }
            catch (System.Exception e)
            {
               log.Error(e.Message, e);
            }
         }

      }

      public void Start()
      {
         if (this.consumerThread == null)
         {
            this.consumerThread = new Thread(this.Run);
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

}
