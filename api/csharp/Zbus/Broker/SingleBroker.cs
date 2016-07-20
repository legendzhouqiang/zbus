
using System;
using System.Threading.Tasks;
using Zbus.Net;
using Zbus.Net.Http;

namespace Zbus.Broker
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
               client.ConnectAsync(config.BrokerAddress).Wait();
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

      public Task<Message> InvokeAsync(Message req)
      {
         MessageClient client = null;
         try
         {
            client = pool.Borrow();
            return client.InvokeAsync(req);
         }
         finally
         {
            if(client != null)
            {
               pool.Return(client);
            } 
         } 
      }

      public Message InvokeSync(Message req)
      {
         MessageClient client = null;
         try
         {
            client = pool.Borrow();
            return client.InvokeSync(req);
         }
         finally
         {
            if (client != null)
            {
               pool.Return(client);
            }
         }
      }

      public Message InvokeSync(Message req, int timeout)
      {
         MessageClient client = null;
         try
         {
            client = pool.Borrow();
            return client.InvokeSync(req, timeout);
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
