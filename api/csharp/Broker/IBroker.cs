using System;

using Zbus.Net;
namespace Zbus.Broker 
{
    public class ClientHint
    {
        public String Mq = "";
        public String Broker = "";
    }

    public interface IBroker : IDisposable
    {
        MessageClient GetClient(ClientHint hint);
        void CloseClient(MessageClient client);
        Message InvokeSync(Message req, int timeout);
    }
}
