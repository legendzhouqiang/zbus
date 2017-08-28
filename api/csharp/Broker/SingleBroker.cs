
using Zbus.Net;
namespace Zbus.Broker
{
    public class SingleBroker : IBroker
    {
        private MessageClientPool pool;
        public SingleBroker(): this(new BrokerConfig())
        { 
        }
        public SingleBroker(BrokerConfig config)
        {
            pool = new MessageClientPool(config);
        }
        public MessageClient GetClient(ClientHint hint)
        {
            return pool.BorrowClient();
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
                //pool = null;
            }
        }
    }
}
