using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using zbus.net;

namespace zbus
{
    public class ClientHint
    {
        public String Mq = "";
	    public String Broker = "";
    }

    public interface Broker : IDisposable
    {
        MessageClient GetClient(ClientHint hint);
        void CloseClient(MessageClient client);
        Message InvokeSync(Message req, int timeout);
    }


    public class BrokerConfig : MessageClientPoolConfig
    {
    }

    public class SingleBroker : Broker
    {
        private MessageClientPool pool;
        public SingleBroker(BrokerConfig config)
        {
            pool = new MessageClientPool(config);
        }
        public MessageClient GetClient(ClientHint hint)
        {
            return pool.BorrowClient() ;
        }

        public void CloseClient(MessageClient client)
        {
            pool.ReturnClient(client);
        }

        public Message InvokeSync(Message req, int timeout)
        {
            MessageClient client = null;
            try
            {
                client = pool.BorrowClient();
                return client.Invoke(req, timeout);
            }
            finally
            {
                if (client != null)
                {
                    pool.ReturnClient(client);
                }
            } 
        }

        public void Dispose()
        {
            if (pool != null)
            {
                pool.Dispose();
                pool = null;
            }
        }
    }
}
