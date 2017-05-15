
using System;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Client.Mq;
using Zbus.Client.Mq.Net;
using Zbus.Client.Net;

namespace Zbus.Client.Mq
{
   public class SingleBroker : IBroker
   {  
      private Pool<MessageClient> pool;  
      public SingleBroker(BrokerConfig config)
      {
         pool = new Pool<MessageClient>
         {
            MaxCount = config.PoolSize,
            Generator = () =>
            {
               MessageClient client = new MessageClient();
               client.Connect(config.BrokerAddress);
               client.StartHeartbeat(config.HeartbeatInterval);
               return client;
            },
         };
      }

      public void CloseInvoker(IMessageInvoker invoker)
      {
         if(invoker is MessageClient)
         {
            pool.Return((MessageClient)invoker);
         } 
      }

      public void Dispose()
      {
         pool.Dispose();
      }

      public IMessageInvoker GetInvoker(ClientHint hint)
      {
         return pool.Borrow(); 
      } 


      public Message Invoke(Message req, int timeout=10000)
      {
         MessageClient client = null;
         try
         {
            client = pool.Borrow();
            return client.Invoke(req, timeout);
         }
         finally
         {
            if (client != null)
            {
               pool.Return(client);
            }
         }
      } 
   }
}
